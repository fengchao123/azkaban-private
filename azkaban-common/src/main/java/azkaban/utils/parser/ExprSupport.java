package azkaban.utils.parser;


import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by janpychou on 1:18 PM.
 * Mail: janpychou@qq.com
 */
public class ExprSupport {

  private static final Object lock = new Object();
  private static final GroovyShell shell;

  private static Map<String, Script> cache = new HashMap<>();

  static {
    CompilerConfiguration cfg = new CompilerConfiguration();
    cfg.setScriptBaseClass(DateParseScript.class.getName());

    shell = new GroovyShell(cfg);
  }

  public static String parseExpr(String expr) {
    Script s = getScriptFromCache(expr);
    return s.run().toString();
  }

  public static String parseExpr(String expr, Map<?, ?> map) {
    Binding binding = new Binding(map);
    Script script = getScriptFromCache(expr);
    script.setBinding(binding);
    return script.run().toString();
  }

  private static Script getScriptFromCache(String expr) {
    if (cache.containsKey(expr)) {
      return cache.get(expr);
    }
    synchronized (lock) {
      if (cache.containsKey(expr)) {
        return cache.get(expr);
      }
      Script script = shell.parse(expr);
      cache.put(expr, script);
      return script;
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    // eg. get one row from db
    Map<String, Object> row = new HashMap<String, Object>();
    row.put("id", 42);
    row.put("name", "");

    //带绑定数据参数的调用方式
    System.out.println(ExprSupport.parseExpr("nvl(id,0)", row));
    System.out.println(ExprSupport.parseExpr("nvl(name,'anonymous')", row));

    //不带绑定数据参数的调用方式，这个是groovy的内置能力
    System.out.println(ExprSupport.parseExpr("1+2"));
//    System.out.println(ExprSupport.parseExpr("2016-15-11", null));
    System.out.println(ExprSupport.parseExpr("nowDate()" , row)); //2016-08-11
    System.out.println(ExprSupport.parseExpr("nowDate(-1)" , row)); //2016-08-11
    System.out.println(ExprSupport.parseExpr("nowDate('yyyy/MM/dd')")); //2016/08/11
    System.out.println(ExprSupport.parseExpr("nowDate(-1, 'yyyy/MM/dd')"));  //2016/08/12
    System.out.println(ExprSupport.parseExpr("nowDate( -1, 'day', 'yyyy/MM/dd',)"));   //2016/08/11 15
    System.out.println(ExprSupport.parseExpr("nowDate( -1, 'hour', 'yyyy/MM/dd HH',)"));   //2016/08/11 15
    System.out.println(ExprSupport.parseExpr("nowDate( 10, 'minute', 'yyyy/MM/dd HH:mm',)")); //2016/08/11 15:02

  }
}