package azkaban.user;

import azkaban.utils.Props;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Created by root on 16-8-10.
 */
public class UserAndGroupManager {
    private static final Logger logger = Logger.getLogger(UserAndGroupManager.class);
    private final UserAndGroupLoader userAndGroupLoader;
    private final Props props;


    public UserAndGroupManager(UserAndGroupLoader loader, Props props) {
        this.userAndGroupLoader = loader;
        this.props = props;
    }

    /**
     * get all user
     */
    public List<Map<String,Object>> getAllUser(String userName,int curPage,int pageSize){
        List<Map<String,Object>> userlist = null;
        try {
            userlist = userAndGroupLoader.getAllUser(userName,curPage,pageSize);
        }catch(UserManagerException e){
            throw new RuntimeException("Could not get all user ", e);
        }
        return userlist;
    }

    public List<Map<String,Object>> getAllGroup(){
        List<Map<String,Object>> grouplist = null;
        try {
            grouplist = userAndGroupLoader.getAllGroup();
        }catch(UserManagerException e){
            throw new RuntimeException("Could not get all group ", e);
        }
        return grouplist;
    }

    public List<Map<String,Object>> getAllRole(){
        List<Map<String,Object>> rolelist = null;
        try {
            rolelist = userAndGroupLoader.getAllRole();
        }catch(UserManagerException e){
            throw new RuntimeException("Could not get all role ", e);
        }
        return rolelist;
    }


    public int getUserByName(String name){
        int count = 0;
        try{
            count = userAndGroupLoader.getUserByName(name);
        }catch(UserManagerException e){
            throw new RuntimeException("find user by  name error!!! ", e);
        }
        return count;
    }

    public void addUser(Map usermap,User user)  {
        try {
            userAndGroupLoader.addUser(usermap,user);
        } catch (UserManagerException e) {
            e.printStackTrace();
        }
    }

    public void editUser(Map usermap,User user)  {
        try {
            userAndGroupLoader.editUser(usermap,user);
        } catch (UserManagerException e) {
            e.printStackTrace();
        }
    }

    public void deleteUser(String name)  {
        try {
            userAndGroupLoader.deleteUser(name);
        } catch (UserManagerException e) {
            e.printStackTrace();
        }
    }
}
