package azkaban.user;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by root on 16-8-10.
 */
public interface UserLoader {

    /**
     * @return
     * @throws UserManagerException
     */
    HashMap<String, User> queryAllUser() throws UserManagerException;

    List<HashMap> queryAllUserAnd() throws UserManagerException;

    /**
     * @return
     * @throws UserManagerException
     */
    HashMap<String, Role> queryRole() throws  UserManagerException;


    /**
     * @return
     * @throws UserManagerException
     */
    HashMap<String, Set<String>>  queryGroup() throws  UserManagerException;

}
