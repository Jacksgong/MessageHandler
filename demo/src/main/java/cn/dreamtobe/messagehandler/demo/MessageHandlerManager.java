import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import org.apache.log4j.Logger;
public class MessageHandlerManager
{
  private static MsgHandlerManager msgHandlerMgr = null;
  private static Object obj = new Object();
  static Logger logger = Logger.getLogger(MsgHandlerManager.class);
  private static Hashtable<String, IMsgHandler> handlers = new Hashtable();

  private MsgHandlerManager()
  {
    try
    {
    
      initMsgHander();
    }
    catch (Exception e)
    {
      logger.error("exception happens when loading handler ", e);
    }
  }

  public static synchronized MsgHandlerManager getInstance()
  {
    if (msgHandlerMgr == null)
      synchronized (obj)
      {
        if (msgHandlerMgr == null)
          msgHandlerMgr = new MsgHandlerManager();
      }
    return msgHandlerMgr;
  }




 
  public synchronized IMsgHandler getMsgHandler(String hname)
     
  {
    IMsgHandler iMsgHandler = null;
    try
    {
      iMsgHandler = (IMsgHandler)handlers.get(hname);
      if (iMsgHandler == null)
      {
     
        initMsgHander();
        iMsgHandler = (IMsgHandler)handlers.get(hname);
        
        }
      }
    catch (Exception e)
    {   logger.error("use default hdr", e);
    	
       }
    return iMsgHandler;
  }

private void initMsgHander()
throws Exception
{
try
{
  Properties properties = new Properties();
  String str2 = ConfigFiles.getInstance().getMsgHandlersFileName();
 // logger.info(str2);
  FileInputStream fileInputStream = new FileInputStream(str2);
  properties.load(fileInputStream);
  Enumeration localEnumeration = properties.keys();
  while (localEnumeration.hasMoreElements())
  {
    String str1 = (String)localEnumeration.nextElement();
    if (handlers.containsKey(str1))
      continue;
    try
    {
    	handlers.put(str1, (IMsgHandler)ObjectFactory.createObject(properties.getProperty(str1)));
    }
    catch (Exception localException2)
    {
    	logger.error("load msg handler error : " + str1, localException2);
    }
  }
  fileInputStream.close();
}
catch (Exception e)
{
  throw e;
}
}
}

