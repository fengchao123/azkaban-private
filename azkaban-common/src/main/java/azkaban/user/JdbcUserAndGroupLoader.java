package azkaban.user;

import azkaban.database.AbstractJdbcLoader;
import azkaban.utils.Props;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by root on 16-8-10.
 */
public class JdbcUserAndGroupLoader extends AbstractJdbcLoader implements UserAndGroupLoader {
    private static final Logger logger = Logger
            .getLogger(JdbcUserAndGroupLoader.class);

    public JdbcUserAndGroupLoader(Props props) {
        super(props);
    }

    @Override
    public List<Map<String, Object>> getAllUser(String userName,int curPage,int pageSize) throws UserManagerException {
        Connection connection = getConnection();
        List<Map<String,Object>> userlist = new ArrayList<Map<String,Object>>();
        userlist = getAllUser(connection,userName,curPage,pageSize);
        return userlist;
    }

    public List<Map<String, Object>> getAllGroup() throws UserManagerException {
        Connection connection = getConnection();
        List<Map<String,Object>> grouplist = new ArrayList<Map<String,Object>>();
        grouplist = getAllGroup(connection);
        return grouplist;
    }

    public List<Map<String, Object>> getAllRole() throws UserManagerException {
        Connection connection = getConnection();
        List<Map<String,Object>> rolelist = new ArrayList<Map<String,Object>>();
        rolelist = getAllRole(connection);
        return rolelist;
    }

    public int getUserByName(String name) throws UserManagerException{
        Connection connection = getConnection();
        int state = 0;
        state = getUserByName(connection,name);
        return state;
    }

    public void addUser(Map usermap,User user) throws UserManagerException {
        Connection connection = getConnection();
        addUser(connection,usermap,user);
    }

    public void editUser(Map usermap,User user) throws UserManagerException {
        Connection connection = getConnection();
        editUser(connection,usermap,user);
    }

    public void deleteUser(String name) throws UserManagerException {
        Connection connection = getConnection();
        deleteUser(connection,name);
    }


    public List<Map<String,Object>> getAllUser(Connection connection,String userName,int curPage,int pageSize) throws UserManagerException{
        List<Map<String,Object>> userlist = new ArrayList<Map<String,Object>>();
        QueryRunner runner = new QueryRunner();
        UserAndGroupLoaderResultHandler handler = new UserAndGroupLoaderResultHandler();
        IntHander inthandler = new IntHander();
        String SELECT_USER_SQL = "";
        String temp_sql = "";
        String total_num_sql = "";
        Map<String,Object> totalnumMap = new HashMap<String,Object>();
        int offset = 0;
        int totalnum = 0;
        try {
            if(userName!=null&&userName!=""){
                temp_sql = " and username like '%"+userName+"%' ";
            }
            if(curPage>1){
                offset = (curPage-1)*pageSize;
            }
            SELECT_USER_SQL = "SELECT username,password,usergroup,roles,lxdh,email from azkaban_users  where 1=1 "+temp_sql+" order by lrrq desc limit "+pageSize+" offset "+offset;
            total_num_sql = "SELECT count(1) as totalnum from azkaban_users  where 1=1 "+temp_sql;
            userlist = runner.query(connection, SELECT_USER_SQL, handler);
            totalnum = runner.query(connection, total_num_sql, inthandler);
            totalnumMap.put("totalnum",totalnum);
            if(userlist!=null) {
                userlist.add(totalnumMap);
            }
        } catch (SQLException e) {
            throw new UserManagerException("Error retrieving all user", e);
        }finally {
            DbUtils.closeQuietly(connection);
        }
        return userlist;
    }

    public List<Map<String,Object>> getAllGroup(Connection connection) throws UserManagerException{
        List<Map<String,Object>> grouplist = new ArrayList<Map<String,Object>>();
        QueryRunner runner = new QueryRunner();
        GroupResultHandler handler = new GroupResultHandler();
        try {
            String sql = " select * from cs_group ";
            grouplist = runner.query(connection,sql, handler);
        } catch (SQLException e) {
            throw new UserManagerException("Error retrieving all group", e);
        }finally {
            DbUtils.closeQuietly(connection);
        }
        return grouplist;
    }

    public List<Map<String,Object>> getAllRole(Connection connection) throws UserManagerException{
        List<Map<String,Object>> rolelist = new ArrayList<Map<String,Object>>();
        QueryRunner runner = new QueryRunner();
        RoleResultHandler handler = new RoleResultHandler();
        try {
            String sql = " select * from cs_role ";
            rolelist = runner.query(connection,sql, handler);
        } catch (SQLException e) {
            throw new UserManagerException("Error retrieving all role", e);
        }finally {
            DbUtils.closeQuietly(connection);
        }
        return rolelist;
    }

    public int getUserByName(Connection connection,String name) throws UserManagerException{
        QueryRunner runner = new QueryRunner();
        IntHander inthandler = new IntHander();
        int state = 0;
        String sql = "";
        try {
            if (name != null && name != "") {
                 sql = " select count(1) as userTotal from azkaban_users where username = '" + name + "'";
            }
            state = runner.query(connection, sql, inthandler);
        }catch (SQLException e) {
                throw new UserManagerException("find user by name error!!!", e);
        }finally {
                DbUtils.closeQuietly(connection);
        }
        return state;
    }





    public void addUser(Connection connection,Map usermap,User user) throws UserManagerException{
        QueryRunner runner = new QueryRunner();
        String name = (String)usermap.get("name");
        String pass = (String)usermap.get("pass");
        String group = (String)usermap.get("group");
        String roles = (String)usermap.get("roles");
        String lxdh = (String)usermap.get("lxdh");
        String email = (String)usermap.get("email");
        String lrrdm = user.getUserId();

        String   sql = "insert into azkaban_users(username,password,usergroup,roles,lxdh,email,lrrq,lrrdm)values('"+name+"','"
                       + pass+"','"+group+"','"+roles+"','"+lxdh+"','"+email+"',now(),'"+lrrdm+"')";

        try {
            int i = runner.update(connection, sql);
            if (i == 0) {
                    throw new UserManagerException("No user have been inserted.");
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw new UserManagerException(
                    "Insert user failed. " , e);
        }
    }

    public void editUser(Connection connection,Map usermap,User user) throws UserManagerException{
        QueryRunner runner = new QueryRunner();
        String name = (String)usermap.get("name");
        String pass = (String)usermap.get("pass");
        String group = (String)usermap.get("group");
        String roles = (String)usermap.get("roles");
        String lxdh = (String)usermap.get("lxdh");
        String email = (String)usermap.get("email");
        String xgrdm = user.getUserId();
        String   sql = " update azkaban_users set password='"+pass+"',usergroup='"+group+"',roles='"+roles+"',lxdh='"
                       + lxdh+"',email='"+email+"',xgrdm='"+xgrdm+"',xgrq=now() where username='"+name+"'";

        try {
            int i = runner.update(connection, sql);
            if (i == 0) {
                throw new UserManagerException("No user have been updated.");
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw new UserManagerException(
                    "Update user failed. " , e);
        }
    }

    public void deleteUser(Connection connection,String name) throws UserManagerException{
        QueryRunner runner = new QueryRunner();

        String sql = " delete from azkaban_users where username='"+name+"'";

        try {
            int i = runner.update(connection, sql);
            if (i == 0) {
                throw new UserManagerException("No user have been deleted.");
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw new UserManagerException(
                    "Delete user failed. " , e);
        }
    }


    private Connection getConnection() throws UserManagerException {
        Connection connection = null;
        try {
            connection = super.getDBConnection(false);
        } catch (Exception e) {
            DbUtils.closeQuietly(connection);
            throw new UserManagerException("Error getting DB connection.", e);
        }

        return connection;
    }

    private static class UserAndGroupLoaderResultHandler implements
            ResultSetHandler<List<Map<String,Object>>> {

        @Override
        public List<Map<String,Object>> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return null;
            }
            ArrayList<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
            do {
                String username = rs.getString(1);
                String password = rs.getString(2);
                String usergroup = (rs.getString(3)!=null)?rs.getString(3):"";
                String roles = (rs.getString(4)!=null)?rs.getString(4):"";
                String lxdh = (rs.getString(5)!=null)?rs.getString(5):"";
                String email = (rs.getString(6)!=null)?rs.getString(6):"";

                Map<String,Object> map = new HashMap<String,Object>();
                map.put("username",username);
                map.put("password",password);
                map.put("usergroup",usergroup);
                map.put("roles",roles);
                map.put("lxdh",lxdh);
                map.put("email",email);

                list.add(map);
            } while (rs.next());
            return list;
        }


    }

    private static class GroupResultHandler implements
            ResultSetHandler<List<Map<String,Object>>> {

        @Override
        public List<Map<String,Object>> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return null;
            }
            ArrayList<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
            do {
                String group = rs.getString(1);


                Map<String,Object> map = new HashMap<String,Object>();
                map.put("group",group);
                list.add(map);
            } while (rs.next());
            return list;
        }


    }

    private static class RoleResultHandler implements
            ResultSetHandler<List<Map<String,Object>>> {

        @Override
        public List<Map<String,Object>> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return null;
            }
            ArrayList<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
            do {
                String role = rs.getString(1);
                Map<String,Object> map = new HashMap<String,Object>();
                map.put("role",role);
                list.add(map);
            } while (rs.next());
            return list;
        }


    }


    private static class IntHander implements ResultSetHandler<Integer> {

        @Override
        public Integer handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return 0;
            }

            return rs.getInt(1);
        }
    }
}
