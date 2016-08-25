package azkaban.utils;

/**
 * Created by root on 16-8-23.
 */


import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.transport.http.CommonsHttpMessageSender;
import org.w3c.dom.Document;

public class DxSendService {
    Logger logger = Logger.getLogger(DxSendService.class);

    private Client client;
    public DxSendService(){
        String url = "http://150.18.30.150:8080/RegionTax_Inter_v4/services/SMSManage?wsdl";
        try {
            client = new Client(new URL(url));
            client.setProperty(CommonsHttpMessageSender.HTTP_TIMEOUT, "10000");
        } catch (MalformedURLException e) {
            logger.error("调用省短信接口的URL("+url+")格式不合法："+e.getMessage());

        } catch (Exception e) {
            logger.error("调用省短信接口时初始化客户端出错："+e.getMessage());
        }
    }

    public Map<String,Object> sendByWS(String sendNum, String content, String sendUser){
        String reqContent = this.getReqContent(sendNum,content,sendUser);
        String methodName = "sendSms";
        Object[] result = null;
        Map<String,Object> map = new HashMap<String,Object>();
        logger.info("开始调用省短信接口的方法,发送内容为：" + reqContent);
        try {
            result = client.invoke(methodName, new Object[]{reqContent});
        } catch (Exception e) {
            logger.error("调用省短信接口的方法"+methodName+"出错："+e.getMessage());
        }

        logger.info("成功调用省短信接口的方法，返回结果为："+result[0]);
        //如果status不为0000，也要抛出异常
        String resContent = (String)result[0];
        Document document = null;
        try {
            document = XMLTools.StringToDocument(resContent);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("初始化XML出错。XML内容：" + resContent + "。错误信息：" + e.getMessage());
        }
        String status = XMLTools.getElementTextContent(document, "status");
        if(!"0000".equals(status)){
            logger.error("status为"+status+"。返回内容："+resContent);
        }
        logger.info("结束调用省短信接口的方法");
        map.put("resContent",resContent);
        map.put("status",status);
        return map;
    }

    public String getReqContent(String sendNum,String content,String sendUser){
        String username = "JY_SFXT";
        String password = "h%THC)(#";
        String sendPort = "1236625211";
        StringBuffer sb = new StringBuffer();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sendTime =sdf.format(new Date());
        sb.append("<?xml version=\"1.0\" encoding=\"GBK\" ?> ");
        sb.append("<root>");
        sb.append("<head>");
        sb.append("<username>"+username+"</username>");
        sb.append("<password>"+password+"</password>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("	<record send_id=\"1\" send_port=\""+sendPort+
                "\" recv_mobile =\""+sendNum+
                "\" msg_content =\""+content+
                "\" send_time=\""+sendTime+
                "\" send_user=\""+sendUser+
                "\" req_application=\"\"/>");
        sb.append("</body>");
        sb.append("</root>");
        return sb.toString();
    }
}
