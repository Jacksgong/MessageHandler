/**
 * Copyright (c) 2016 Jacksgong(blog.dreamtobe.cn).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.dreamtobe.messagehandler;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Jacksgong on 1/13/16.
 * <p/>
 * support: pause, resume, stop
 * performance: use shallow clone instead of lock
 * why not extends Handler directly? so many methods relate pause and resume operate are final or hide.
 * <p/>
 * Tips: All method thread safe
 */
public class MessageHandler {

    private final static String TAG = "MessageHandler";
    private boolean isDead;
    private volatile boolean isPause;
    private ArrayList<MessageHolder> messageHolderList = new ArrayList<>();

    private static class DispatchHandler extends Handler {
        private WeakReference<MessageHandler> messageHandlerWeakReference;

        public DispatchHandler(WeakReference<MessageHandler> messageHandlerWeakReference) {
            this.messageHandlerWeakReference = messageHandlerWeakReference;
        }

        @Override
        public void dispatchMessage(Message msg) {
            if (messageHandlerWeakReference == null
                    || messageHandlerWeakReference.get() == null) {
                return;
            }

            if (messageHandlerWeakReference.get().dispatchMessage(msg)) {
                return;
            }

            super.dispatchMessage(msg);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (messageHandlerWeakReference == null
                    || messageHandlerWeakReference.get() == null) {
                return;
            }

            messageHandlerWeakReference.get().handleMessage(msg);
        }

        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            if (messageHandlerWeakReference == null
                    || messageHandlerWeakReference.get() == null) {
                return false;
            }

            if (messageHandlerWeakReference.get().dispatchSendMessage(msg, uptimeMillis)) {
                return false;
            }

            return super.sendMessageAtTime(msg, uptimeMillis);
        }
    }

    private DispatchHandler handler;

    public MessageHandler() {
        handler = new DispatchHandler(new WeakReference<>(this));
        isDead = false;
        isPause = false;
    }

    /**
     * for guarantee pause and killSelf in effect, final this method
     * if you need, consider implement {@link #handleMessage(Message)}
     *
     * @return is consumed
     */
    final public boolean dispatchMessage(Message msg) {
        if (isDead) {
            return true;
        }

        // pause
        if (isPause) {
            return true;
        }

        final ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
        for (MessageHolder messageHolder : list) {
            if (messageHolder.msg == msg) {
                messageHolderList.remove(messageHolder);
                break;
            }
        }

        return false;
    }

    /**
     * @return is consumed
     */
    public boolean dispatchSendMessage(Message msg, long uptimeMillis) {
        boolean consumed;

        do {
            if (isDead) {
                consumed = true;
                break;
            }

            MessageHolder messageHolder = new MessageHolder(msg, uptimeMillis);

            if (isPause) {
                consumed = true;
                messageHolder.stop();
            } else {
                consumed = false;
            }

            messageHolderList.add(messageHolder);
            break;

        } while (false);


        return consumed;
    }

    public void handleMessage(Message msg) {
    }

    // ----------------------------------------------------------
    /**
     * pause and hold all message
     */
    public void pause() {
        if (isPause) {
            return;
        }
        isPause = true;
        final ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
        for (MessageHolder messageHolder : list) {
            messageHolder.stop();
        }
        Log.d(TAG, String.format("pause %d", list.size()));
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * resume message
     */
    public void resume() {
        if (!isPause) {
            return;
        }
        isPause = false;
        final ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
        messageHolderList.clear();
        for (MessageHolder messageHolder : list) {
            messageHolder.resume();
            handler.sendMessageDelayed(messageHolder.msg, messageHolder.delay);
        }

        Log.d(TAG, String.format("resume %d", list.size()));
    }

    public boolean isPaused(){
        return this.isPause;
    }

    public boolean isDead(){
        return this.isDead;
    }

    /**
     * cancel all message send by this handler
     */
    public void cancelAllMessage() {
        messageHolderList.clear();
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * this handler do not valid anymore
     */
    public void killSelf() {
        isDead = true;
        cancelAllMessage();
    }

    /**
     * @see Handler#sendEmptyMessage(int)
     */
    public boolean sendEmptyMessage(int what) {
        return handler.sendEmptyMessage(what);
    }

    /**
     * @see Handler#sendEmptyMessageDelayed(int, long)
     */
    public boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        return handler.sendEmptyMessageDelayed(what, delayMillis);
    }

    /**
     * @see Handler#sendMessage(Message)
     */
    public boolean sendMessage(Message msg) {
        return handler.sendMessage(msg);
    }

    /**
     * @see Handler#sendMessageDelayed(Message, long)
     */
    public boolean sendMessageDelayed(Message msg, long delayMillis) {
        return handler.sendMessageDelayed(msg, delayMillis);
    }


    /**
     * @see Handler#sendEmptyMessageAtTime(int, long)
     */
    public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
        return handler.sendMessageAtTime(msg, uptimeMillis);
    }

    /**
     * @see Handler#sendMessageAtFrontOfQueue(Message)
     */
    public boolean sendMessageAtFrontOfQueue(Message msg) {
        if (dispatchSendMessage(msg, 0)) {
            return false;
        }
        return handler.sendMessageAtFrontOfQueue(msg);
    }

    /**
     * @see Handler#post(Runnable)
     */
    public final boolean post(Runnable r) {
        return handler.post(r);
    }

    /**
     * @see Handler#postDelayed(Runnable, long)
     */
    public boolean postDelayed(Runnable r, long delayMillis) {
        return handler.postAtTime(r, delayMillis);
    }

    //TODO support runWithScissors & postAtFrontOfQueue

    /**
     * @see Handler#removeMessages(int)
     */
    public void removeMessages(int what) {
        ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
        for (MessageHolder messageHolder : list) {
            if (messageHolder.msg.what == what) {
                if (messageHolder != null) {
                    messageHolderList.remove(messageHolder);
                }
            }
        }

        handler.removeMessages(what);
    }

    /**
     * @see Handler#removeCallbacks(Runnable)
     */
    public final void removeCallbacks(Runnable r) {
        ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
        for (MessageHolder messageHolder : list) {
            if (messageHolder.msg.getCallback() == r) {
                if (messageHolder != null) {
                    messageHolderList.remove(messageHolder);
                }
            }
        }

        handler.removeCallbacks(r);
    }

    /**
     * @see Handler#obtainMessage()
     */
    public Message obtainMessage() {
        return handler.obtainMessage();
    }


    public static class MessageHolder {
        private Message msg;
        private long upTimeMills;

        private long delay;

        public MessageHolder(final Message msg, final long upTimeMills) {
            this.msg = msg;
            this.upTimeMills = upTimeMills;
        }

        public void stop() {
            delay = this.upTimeMills - SystemClock.uptimeMillis();
        }

        public void resume() {
            delay = Math.min(0, delay);
        }
    }
}
