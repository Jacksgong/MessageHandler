/*
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
import android.os.Looper;
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

    private boolean isDead;
    private volatile boolean isPause;

    private final MessageHolderList list = new MessageHolderList();

    private static class DispatchHandler extends Handler {
        private final WeakReference<MessageHandler> messageHandlerWeakReference;

        DispatchHandler(WeakReference<MessageHandler> messageHandlerWeakReference) {
            this.messageHandlerWeakReference = messageHandlerWeakReference;
        }

        DispatchHandler(WeakReference<MessageHandler> messageHandlerWeakReference, Callback callback) {
            super(callback);
            this.messageHandlerWeakReference = messageHandlerWeakReference;
        }

        DispatchHandler(WeakReference<MessageHandler> messageHandlerWeakReference, Looper looper) {
            this(messageHandlerWeakReference, looper, null);
        }

        DispatchHandler(WeakReference<MessageHandler> messageHandlerWeakReference, Looper looper,
                        Callback callback) {
            super(looper, callback);
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

            return !messageHandlerWeakReference.get().dispatchSendMessage(msg, uptimeMillis) // not consumed
                    && super.sendMessageAtTime(msg, uptimeMillis);

        }
    }

    private final DispatchHandler handler;

    public MessageHandler() {
        handler = new DispatchHandler(new WeakReference<>(this));
        isDead = false;
        isPause = false;
    }

    /**
     * @param looper The looper, must not be null!
     * @see Handler#Handler(Looper)
     */
    public MessageHandler(final Looper looper) {
        handler = new DispatchHandler(new WeakReference<>(this), looper);
        isDead = false;
        isPause = false;
    }

    /**
     * @param callback The callback interface in which to handle messages, or null.
     * @see Handler#Handler(Handler.Callback)
     */
    public MessageHandler(final Handler.Callback callback) {
        handler = new DispatchHandler(new WeakReference<>(this), callback);
        isDead = false;
        isPause = false;
    }

    /**
     * @param looper   The looper, must not be null!
     * @param callback The callback interface in which to handle messages, or null.
     * @see Handler#Handler(Looper, Handler.Callback)
     */
    public MessageHandler(final Looper looper, final Handler.Callback callback) {
        handler = new DispatchHandler(new WeakReference<>(this), looper, callback);
        isDead = false;
        isPause = false;
    }

    /**
     * for guarantee pause and killSelf in effect, private this method
     * if you need, consider implement {@link #handleMessage(Message)}
     *
     * @return is consumed
     */
    private boolean dispatchMessage(Message msg) {
        logD("dispatchMessage %B %B %d", isDead, isPause, list.size());
        if (isDead) {
            return true;
        }

        // pause
        if (isPause) {
            return true;
        }

        list.remove(msg);

        return false;
    }

    /**
     * @return is consumed
     */
    private boolean dispatchSendMessage(Message msg, long uptimeMillis) {
        logD("dispatchSendMessage %B %B %d", isDead, isPause, list.size());
        boolean consumed;

        //noinspection LoopStatementThatDoesntLoop
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

            list.add(messageHolder);

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

        final ArrayList<MessageHolder> cloneList = list.clone();
        for (MessageHolder messageHolder : cloneList) {
            messageHolder.stop();
        }
        logD("pause %d", cloneList.size());
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
        final ArrayList<MessageHolder> cloneList = list.clone();
        list.clearButHoldMessage();
        for (MessageHolder messageHolder : cloneList) {
            messageHolder.resume();
            handler.sendMessageDelayed(messageHolder.msg, messageHolder.delay);
        }

        logD("resume %d", cloneList.size());
    }

    public boolean isPaused() {
        return this.isPause;
    }

    public boolean isDead() {
        return this.isDead;
    }

    /**
     * cancel all message send by this handler
     */
    public void cancelAllMessage() {
        logD("cancelAllMessage %B %B %d", isDead, isPause, list.size());
        list.clear();
        handler.removeCallbacksAndMessages(null);
    }

    /**
     * this handler do not valid anymore
     */
    public void killSelf() {
        logD("killSelf %B %B %d", isDead, isPause, list.size());
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
        return !dispatchSendMessage(msg, 0) // not consumed
                && handler.sendMessageAtFrontOfQueue(msg);
    }

    /**
     * @see Handler#post(Runnable)
     */
    public boolean post(Runnable r) {
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
        list.remove(what);
        handler.removeMessages(what);
    }

    /**
     * @see Handler#removeCallbacks(Runnable)
     */
    public void removeCallbacks(Runnable r) {
        list.remove(r);
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
        private final long upTimeMills;

        private final Message compareMsg;

        private long delay;

        public MessageHolder(final Message msg, final long upTimeMills) {
            // Message may recycle by Looper#looper/MessageQueue#removexxx
            this.compareMsg = msg;
            // Message will not be recycle by android framework, so safe.
            this.msg = Message.obtain(msg);
            this.upTimeMills = upTimeMills;
        }

        public void stop() {
            delay = this.upTimeMills - SystemClock.uptimeMillis();
        }

        public void resume() {
            delay = Math.min(0, delay);
        }

        public void dead() {
            if (msg != null) {
                synchronized (this) {
                    if (msg != null) {
                        // flag must be clear, free to recycle.
                        msg.recycle();
                        // have to set nil, because the msg will be used in other places by recycle pool.
                        msg = null;
                    }

                }
            }

        }

        public boolean compare(final Message msg) {
            return this.compareMsg == msg;
        }

        public boolean compare(final int what) {
            return this.msg.what == what;
        }

        public boolean compare(final Runnable runnable) {
            return this.msg.getCallback() == runnable;
        }

    }

    /**
     * why this? for being good for Message recycle or not recycle.
     */
    private static class MessageHolderList {
        private final ArrayList<MessageHolder> messageHolderList = new ArrayList<>();

        boolean add(final Message msg, final long delay) {
            return add(new MessageHolder(msg, delay));
        }

        boolean add(MessageHolder holder) {
            logD("List:Change %d + 1 add", size());
            return messageHolderList.add(holder);
        }

        private boolean remove(MessageHolder holder) {
            logD("List:Change %d - 1 remove", size());
            return messageHolderList.remove(holder);
        }

        boolean remove(final int what) {
            @SuppressWarnings("unchecked") ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
            for (MessageHolder messageHolder : list) {
                if (messageHolder.compare(what)) {
                    messageHolder.dead();
                    return remove(messageHolder);
                }
            }

            return false;
        }

        boolean remove(final Runnable callback) {
            @SuppressWarnings("unchecked") ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
            for (MessageHolder messageHolder : list) {
                if (messageHolder.compare(callback)) {
                    messageHolder.dead();
                    return remove(messageHolder);
                }
            }

            return false;
        }

        boolean remove(final Message msg) {
            @SuppressWarnings("unchecked") ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
            for (MessageHolder messageHolder : list) {
                if (messageHolder.compare(msg)) {
                    messageHolder.dead();
                    return remove(messageHolder);
                }
            }

            return false;
        }

        public void clear() {
            @SuppressWarnings("unchecked") ArrayList<MessageHolder> list = (ArrayList<MessageHolder>) messageHolderList.clone();
            clearButHoldMessage();
            for (MessageHolder messageHolder : list) {
                messageHolder.dead();
            }
        }

        /**
         * natural: in case of will be recycle by system framework, such as: will invoke sendMessage
         * and Looper#looper will invoke recycleUnchecked to recycle Message.
         */
        public void clearButHoldMessage() {
            logD("List:Change %d = 0 clear", size());
            messageHolderList.clear();
        }

        @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneDoesntDeclareCloneNotSupportedException"})
        @Override
        protected ArrayList<MessageHolder> clone() {
            //noinspection unchecked
            return (ArrayList<MessageHolder>) messageHolderList.clone();
        }

        public int size(){
            return messageHolderList.size();
        }
    }

    private final static String TAG = "MessageHandler";
    public static boolean NEED_LOG = false;

    private static void logD(final String msg, final Object... args) {
        if (!NEED_LOG) {
            return;
        }
        Log.d(TAG, String.format(msg, args));
    }

}
