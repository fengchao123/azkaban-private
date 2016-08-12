package azkaban.webapp.servlet;

import azkaban.executor.ExecutableFlow;
import azkaban.executor.ExecutorManagerAdapter;
import azkaban.executor.ExecutorManagerException;
import azkaban.project.ProjectManagerException;
import azkaban.server.session.Session;
import azkaban.user.User;
import azkaban.user.UserAndGroupManager;
import azkaban.user.UserManager;
import azkaban.user.UserManagerException;
import azkaban.webapp.AzkabanWebServer;
import org.apache.log4j.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by root on 16-8-9.
 */
public class UserManagerServlet extends LoginAbstractAzkabanServlet {
    private static final Logger logger = Logger.getLogger(UserManagerServlet.class
            .getName());

    private UserAndGroupManager userAndGroupManager;
    private ExecutorManagerAdapter executorManager;
    private UserManager userManager;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        AzkabanWebServer server = (AzkabanWebServer) getApplication();
        userAndGroupManager = server.getUserAndGroupManager();
        userManager = server.getUserManager();
    }

    @Override
    protected void handleGet(HttpServletRequest req, HttpServletResponse resp, Session session) throws ServletException, IOException {
        handlePageRender(req, resp, session);

    }

    @Override
    protected void handlePost(HttpServletRequest req, HttpServletResponse resp, Session session) throws ServletException, IOException {

    }

    private void handlePageRender(HttpServletRequest req, HttpServletResponse resp, Session session) {
        User user = session.getUser();
        Page page = newPage(req, resp, session, "azkaban/webapp/servlet/velocity/userManager.vm");
        int pageNum = getIntParam(req, "page", 1);
        int pageSize = getIntParam(req, "size", 10);
        HashMap<String, Object> ret = new HashMap<>();
        if (pageNum < 0) {
            pageNum = 1;
        }
        String type= req.getParameter("type");
        if ("group".equals(type)) {
            page.add("viewProjects", "group");
        }else if("addUser".equals(type)){
            try {
                String name = getParam(req, "name");
                String pass = getParam(req, "pass");
                String group = getParam(req, "group");
                String roles = getParam(req, "roles");
                String lxdh = getParam(req, "lxdh");
                String email = getParam(req, "email");
                Map map = new HashMap();
                map.put("name",name);
                map.put("pass",pass);
                map.put("group",group);
                map.put("roles",roles);
                map.put("lxdh",lxdh);
                map.put("email",email);
                User user1 = new User(name);
                user1.setPassword(pass);
                user1.addGroup(group);
                user1.addRole(roles);
                user1.setLxdh(lxdh);
                user1.setEmail(email);
                addUser(req,ret,map,user,user1);
                this.writeJSON(resp, ret);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else if("editUser".equals(type)){
            try {
                String name = getParam(req, "name");
                String pass = getParam(req, "pass");
                String group = getParam(req, "group");
                String roles = getParam(req, "roles");
                String lxdh = getParam(req, "lxdh");
                String email = getParam(req, "email");
                Map map = new HashMap();
                map.put("name",name);
                map.put("pass",pass);
                map.put("group",group);
                map.put("roles",roles);
                map.put("lxdh",lxdh);
                map.put("email",email);
                User user1 = new User(name);
                user1.setPassword(pass);
                user1.addGroup(group);
                user1.addRole(roles);
                user1.setLxdh(lxdh);
                user1.setEmail(email);
                editUser(req,ret,map,user,user1);
                this.writeJSON(resp, ret);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        else if("deleteUser".equals(type)){
            try {
                String name = getParam(req, "name");
                deleteUser(req,ret,name);
                this.writeJSON(resp, ret);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        else{
            int curPage = 1;
            int totalnum = 0;
            int totalpage = 0;
            String userName = "";
            String curPage_str = req.getParameter("curPage");
           // if(hasParam(req,"doaction")) {
                userName = req.getParameter("searchterm");
          //  }
            if(userName==null){
                userName = "";
            }
            if(curPage_str!=null&&curPage_str!="") {
                curPage = Integer.parseInt(curPage_str);
            }
            List<Map<String,Object>> list = userAndGroupManager.getAllUser(userName,pageNum,pageSize);
            List<Map<String,Object>> grouplist = userAndGroupManager.getAllGroup();
            List<Map<String,Object>> roleslist = userAndGroupManager.getAllRole();
            if(list!=null) {
                int count = list.size();
                for (int i = 0; i < count; i++) {
                    Map<String, Object> listmap = list.get(i);
                    if (listmap.containsKey("totalnum")) {
                        totalnum = (int) listmap.get("totalnum");
                        if(totalnum<=10){
                            totalpage = 1;
                        }else {
                            totalpage = (totalnum%10 > 0) ? totalnum/10 + 1 : totalnum/10;
                        }
                        list.remove(i);
                    }
                }
            }
            if (pageNum == 1) {
                page.add("previous", new PageSelection(1, pageSize, true, false));
            } else {
                page.add("previous", new PageSelection(pageNum - 1, pageSize, false,
                        false));
            }
            page.add("next", new PageSelection(pageNum + 1, pageSize, false, false));
            // Now for the 5 other values.
            int pageStartValue = 1;
            if (pageNum > 3) {
                pageStartValue = pageNum - 2;
            }


            page.add("page1", new PageSelection(pageStartValue, pageSize, false,
                    pageStartValue==pageNum));
            pageStartValue++;
            page.add("page2", new PageSelection(pageStartValue, pageSize, false,
                    pageStartValue == pageNum));
            pageStartValue++;
            page.add("page3", new PageSelection(pageStartValue, pageSize, false,
                    pageStartValue == pageNum));
            pageStartValue++;
            page.add("page4", new PageSelection(pageStartValue, pageSize, false,
                    pageStartValue == pageNum));
            pageStartValue++;
            page.add("page5", new PageSelection(pageStartValue, pageSize, false,
                    pageStartValue == pageNum));
            pageStartValue++;

            page.add("userlist",list);
            page.add("grouplist",grouplist);
            page.add("roleslist",roleslist);
            page.add("size", pageSize);
            page.add("page", pageNum);
            page.add("totalnum",totalnum);
            page.add("totalpage",totalpage);
            page.add("search_term",userName);
            page.add("viewProjects", "user");
        }
        page.add("user",user);
        page.render();
    }

    public void addUser(HttpServletRequest req,HashMap<String, Object> ret,Map usermap,User user,User user1) throws ServletException{
        String name = (String)usermap.get("name");
        String pass = (String)usermap.get("pass");
        String role = (String)usermap.get("roles");
        if (name == null || name.trim().isEmpty()) {
            ret.put("error", "用户名不能为空");
            return;
        } else if (pass == null || pass.trim().isEmpty()) {
            ret.put("error", "密码不能为空");
            return;
        }else if (role == null || role.trim().isEmpty()) {
            ret.put("error", "role不能为空");
            return;
        }
        int state = userAndGroupManager.getUserByName(name);
        if (state>0) {
            ret.put("error", "The user already exists!!!");
            return;
        } else {
            try {

                userAndGroupManager.addUser(usermap,user);
                userManager.addUser(user1);
            } catch (Exception e) {
                ret.put("error", e.getMessage());
            }

        }
    }

    public void deleteUser(HttpServletRequest req,HashMap<String, Object> ret,String name) throws ServletException{
        try {
            /*
            int pageNum = 1;
            int pageSize = 10;
            List<Map<String,Object>> list = userAndGroupManager.getAllUser(name,pageNum,pageSize);
            Map<String,Object> map = list.get(0);
            String username = (String)map.get("username");
            String password = (String)map.get("password");
            String usergroup = (String)map.get("usergroup");
            String roles = (String)map.get("roles");
            String lxdh = (String)map.get("lxdh");
            String email = (String)map.get("email");
            User user1 = new User(username);
            user1.setPassword(password);
            user1.addGroup(usergroup);
            user1.addRole(roles);
            user1.setLxdh(lxdh);
            user1.setEmail(email);
            */
            userAndGroupManager.deleteUser(name);
            userManager.removeUser(name);
        } catch (Exception e) {
            ret.put("error", e.getMessage());
        }
    }

    public void editUser(HttpServletRequest req,HashMap<String, Object> ret,Map usermap,User user,User user1) throws ServletException{
        String name = (String)usermap.get("name");
        String pass = (String)usermap.get("pass");
        String role = (String)usermap.get("roles");
        if (name == null || name.trim().isEmpty()) {
            ret.put("error", "用户名不能为空");
            return;
        } else if (pass == null || pass.trim().isEmpty()) {
            ret.put("error", "密码不能为空");
            return;
        }else if (role == null || role.trim().isEmpty()) {
            ret.put("error", "role不能为空");
            return;
        }
        int state = userAndGroupManager.getUserByName(name);
        if (state>0) {
            try {
                userAndGroupManager.editUser(usermap,user);
                userManager.editUser(user1);
            } catch (Exception e) {
                ret.put("error", e.getMessage());
            }
        } else {
            ret.put("error", "The user not exists!!!");
            return;
        }
    }

    public class PageSelection {
        private int page;
        private int size;
        private boolean disabled;
        private boolean selected;

        public PageSelection(int page, int size, boolean disabled, boolean selected) {
            this.page = page;
            this.size = size;
            this.disabled = disabled;
            this.setSelected(selected);
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public boolean getDisabled() {
            return disabled;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
}
