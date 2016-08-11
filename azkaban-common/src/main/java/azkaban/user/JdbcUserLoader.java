package azkaban.user;

import azkaban.database.AbstractJdbcLoader;
import azkaban.utils.Props;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by root on 16-8-10.
 */
public class JdbcUserLoader  extends AbstractJdbcLoader  implements UserLoader{


    private static final Logger logger = Logger
            .getLogger(JdbcUserLoader.class);

    private static final int CHUCK_SIZE = 1024 * 1024 * 10;

    private EncodingType defaultEncodingType = EncodingType.GZIP;

    public EncodingType getDefaultEncodingType() {
        return defaultEncodingType;
    }

    public void setDefaultEncodingType(EncodingType defaultEncodingType) {
        this.defaultEncodingType = defaultEncodingType;
    }

    public JdbcUserLoader(Props props) {
        super(props);
    }

//    private Connection getConnection() throws UserManagerException {
//        Connection connection = null;
//        try {
//            connection = super.getDBConnection(false);
//        } catch (Exception e) {
//            DbUtils.closeQuietly(connection);
//            throw new UserManagerException("Error getting DB connection.", e);
//        }
//
//        return connection;
//    }


    public static class UserResultsHandler implements
            ResultSetHandler<HashMap<String,User>> {
        private static String  QUERY_USERS =
                "select username,password,usergroup,roles,lxdh,email from azkaban_users";

        @Override
        public HashMap<String,User> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return null;
            }
            HashMap<String, User> users = new HashMap<String, User>();
            User user ;

            do{
                String username = rs.getString(1);
                String password = rs.getString(2);
                String group = rs.getString(3);
                String roles = rs.getString(4);
                String lxdh = rs.getString(5);
                String email = rs.getString(6);

//                userPassword.put(username, password);

                // Add the user
                user = new User(username);
                user.addGroup(group);
                user.addRole(roles);
                user.setPassword(password);
                user.setLxdh(lxdh);
                user.setEmail(email);

                users.put(username, user);
                logger.info("Loading user " + user.getUserId());

            }while (rs.next());
            return users;
        }

    }

    public static class UserAllResultsHandler implements
            ResultSetHandler<List<HashMap> > {
        private static String  QUERY_USERSALL =
                "select username,password,usergroup,roles,lxdh,email from azkaban_users";

        @Override
        public List<HashMap> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return null;
            }
            HashMap<String, User> users = new HashMap<String, User>();
            HashMap<String, String> userPassword = new HashMap<String, String>();
            HashMap<String, Set<String>> proxyUserMap =  new HashMap<String, Set<String>>();
            User user ;
            List<HashMap> list = new ArrayList<>();
            do{
                String username = rs.getString(1);
                String password = rs.getString(2);
                String group = rs.getString(3);
                String roles = rs.getString(4);
                String lxdh = rs.getString(5);
                String email = rs.getString(6);


                // Add the user
                user = new User(username);
                user.addGroup(group);
                user.addRole(roles);
                user.setPassword(password);
                user.setLxdh(lxdh);
                user.setEmail(email);

                users.put(username, user);
                logger.info("Loading user " + user.getUserId());
                userPassword.put(username, password);

                    Set<String> proxySet = proxyUserMap.get(username);
                    if (proxySet == null) {
                        proxySet = new HashSet<String>();
                        proxyUserMap.put(username, proxySet);
                    }

                    proxySet.add(username);


            }while (rs.next());
            list.add(users);
            list.add(userPassword);
            list.add(proxyUserMap);
            return list;
        }

    }

    public static class RolesResultsHandler implements
            ResultSetHandler<HashMap<String,Role>> {
        private static String  QUERY_ROLES =
                "select name,permissions from cs_role";

        @Override
        public HashMap<String,Role> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                throw new RuntimeException(
                        "Error loading role. The role 'name' attribute doesn't exist");
//                return null;
            }
            HashMap<String, Role> roles = new HashMap<String, Role>();

            Role role ;

            do{
                String roleName = rs.getString(1);
                String permissions = rs.getString(2);

                String[] permissionSplit = permissions.split("\\s*,\\s*");

                Permission perm = new Permission();
                for (String permString : permissionSplit) {
                    try {
                        Permission.Type type = Permission.Type.valueOf(permString);
                        perm.addPermission(type);
                    } catch (IllegalArgumentException e) {
                        logger.error("Error adding type " + permString
                                + ". Permission doesn't exist.", e);
                    }
                }

                role = new Role(roleName, perm);
                roles.put(roleName, role);


            }while (rs.next());
            return roles;
        }

    }

    public static class GroupResultsHandler implements
            ResultSetHandler<HashMap<String,Set<String>>> {
        private static String  QUERY_GROUPS =
                "select usergroup from cs_group";

        @Override
        public HashMap<String,Set<String>> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                throw new RuntimeException(
                        "Error loading role. The group 'name' attribute doesn't exist");
            }
            HashMap<String, Set<String>> groupRoles = new HashMap<String, Set<String>>();

            Set<String> roleSet = new HashSet<String>();

            do{
                String usergroup = rs.getString(1);

                String[] roleSplit = usergroup.split("\\s*,\\s*");
                for (String role : roleSplit) {
                    roleSet.add(role);
                }


                groupRoles.put(usergroup, roleSet);
                logger.info("Group roles " + usergroup + " added.");

            }while (rs.next());
            return groupRoles;
        }

    }

    @Override
    public HashMap<String, User> queryAllUser() throws UserManagerException {

        QueryRunner runner = createQueryRunner();
        UserResultsHandler userResultsHandler = new UserResultsHandler();
//        Connection connection = getConnection();
        HashMap<String, User>  userList;
        try {
            userList = runner.query(UserResultsHandler.QUERY_USERS,userResultsHandler);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new UserManagerException(
                    "Error query User " , e);
        }

        return userList;
    }

    @Override
    public List<HashMap> queryAllUserAnd() throws UserManagerException {
        QueryRunner runner = createQueryRunner();
        UserAllResultsHandler userAllResultsHandler = new UserAllResultsHandler();
//        Connection connection = getConnection();
        List<HashMap>  userList;
        try {
            userList = runner.query(UserAllResultsHandler.QUERY_USERSALL,userAllResultsHandler);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new UserManagerException(
                    "Error query User " , e);
        }

        return userList;
    }

    @Override
    public HashMap<String, Role> queryRole() throws UserManagerException {

        QueryRunner runner = createQueryRunner();
        RolesResultsHandler RolesResultsHandler = new RolesResultsHandler();

        HashMap<String, Role>  roleList;
        try {
            roleList = runner.query(RolesResultsHandler.QUERY_ROLES,RolesResultsHandler);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new UserManagerException(
                    "查询Role失败 " , e);
        }

        return roleList;
    }

    @Override
    public HashMap<String, Set<String>> queryGroup() throws UserManagerException {

        QueryRunner runner = createQueryRunner();
        GroupResultsHandler GroupsResultsHandler = new GroupResultsHandler();

        HashMap<String, Set<String>>  groups;
        try {
            groups = runner.query(GroupResultsHandler.QUERY_GROUPS,GroupsResultsHandler);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new UserManagerException(
                    "查询Group失败 " , e);
        }

        return groups;
    }


}
