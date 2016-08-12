package azkaban.user;

import java.util.List;
import java.util.Map;
/**
 * Created by root on 16-8-10.
 */
public interface UserAndGroupLoader {
    public List<Map<String,Object>> getAllUser(String userName,int curPage,int pageSize) throws UserManagerException;
    public List<Map<String,Object>> getAllGroup() throws UserManagerException;
    public List<Map<String,Object>> getAllRole() throws UserManagerException;
    public int getUserByName(String name) throws UserManagerException;
    public void addUser(Map usermap,User user) throws UserManagerException;
    public void editUser(Map usermap,User user) throws UserManagerException;
    public void deleteUser(String name) throws UserManagerException;
}
