package azkaban.utils;

/**
 * Created by root on 16-8-23.
 */
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XMLTools {

    /**
     * ObjToXML 方法
     * <p>对象转换为XML</p>
     * @param obj
     * @param map
     * @param header
     * @return
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public static String ObjToXML(Object obj,Map<String, Class<? extends Object>> map,String header){
        XStream stream = new XStream();
        stream.registerConverter(new DateConverter("yyyy-MM-dd",  new String[] {},false));

        Set<String> keySet = map.keySet();
        for(String key : keySet){
            stream.alias(key, map.get(key));
        }

        String result = header + stream.toXML(obj);

        return result;
    }


    /**
     * MapToXML 方法
     * <p></p>
     * @param map
     * @return
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public static String MapToXML(Map<String,Object> map){
        XStream stream = new XStream();
        stream.registerConverter(new DateConverter("yyyy-MM-dd",  new String[] {},false));
//		stream.registerConverter(new XStreamMapToObjConverter());

        String result = stream.toXML(map);
        return result;
    }


    /**
     * XMLToObj 方法
     * <p></p>
     * @param xml
     * @param map
     * @return
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public static Object XMLToObj(String xml,Map<String, Class<? extends Object>> map){
        XStream stream = new XStream(new DomDriver());
        stream.registerConverter(new DateConverter("yyyy-MM-dd",  new String[] {},false));

        Set<String> keySet = map.keySet();
        for(String key : keySet){
            stream.alias(key, map.get(key));
        }

        Object result = stream.fromXML(xml);
        return result;
    }


    /**
     * StringToDocument 方法
     * <p></p>
     * @param xmlString
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public static Document StringToDocument(String xmlString) throws ParserConfigurationException, SAXException, IOException {
        InputSource is = new InputSource(new StringReader(xmlString));
        Document document;
        document = XMLUtils.newDocument(is);
        if (is.getByteStream() != null) {
            is.getByteStream().close();
        } else if (is.getCharacterStream() != null) {
            is.getCharacterStream().close();
        }

        return document;
    }


    /**
     * getElementTextContent 方法
     * <p></p>
     * @param document
     * @param elementName
     * @return
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public static String getElementTextContent(Document document, String elementName) {

        NodeList statusNode = document.getElementsByTagName(elementName);
        String elementTextContent = statusNode.getLength() > 0 ? statusNode.item(0).getTextContent() : "";

        return elementTextContent;
    }


    /**
     * getSubNodeMapMap 方法
     * <p></p>
     * @param rootNode
     * @return
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public Map<String, Map<String, String>> getSubNodeMapMap(Node rootNode) {
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        NodeList childNodes = rootNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                map.put(node.getNodeName(), this.getSubNodeMap(node));
            }
        }
        return map;
    }


    /**
     * getSubNodeMapList 方法
     * <p></p>
     * @param rootNode
     * @return
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public List<Map<String, String>> getSubNodeMapList(Node rootNode) {
        List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        NodeList childNodes = rootNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                list.add(this.getSubNodeMap(node));
            }
        }
        return list;
    }


    /**
     * getSubNodeMap 方法
     * <p></p>
     * @param rootNode
     * @return
     * @author cairuoyu
     * @date 2015年8月12日
     */
    public Map<String, String> getSubNodeMap(Node rootNode) {
        Map<String, String> map = new HashMap<String, String>();
        NodeList childNodes = rootNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                map.put(node.getNodeName(), node.getTextContent());
            }
        }
        return map;
    }


    public static void main(String[] arsg){
        Map<String, Object> map = new HashMap<String,Object>();
        map.put("a", 1);
        map.put("b", 2);
        String mapToXML = XMLTools.MapToXML(map);
        System.out.println(mapToXML);
    }

}
