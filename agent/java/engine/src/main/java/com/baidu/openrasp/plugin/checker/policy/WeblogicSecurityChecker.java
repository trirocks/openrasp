package com.baidu.openrasp.plugin.checker.policy;

import com.baidu.openrasp.HookHandler;
import com.baidu.openrasp.plugin.checker.CheckParameter;
import com.baidu.openrasp.plugin.info.EventInfo;
import com.baidu.openrasp.plugin.info.SecurityPolicyInfo;
import com.baidu.openrasp.tool.Reflection;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * @description: weblogic基线检查
 * @author: anyang
 * @create: 2018/09/11 11:12
 */
public class WeblogicSecurityChecker extends PolicyChecker {
    private static final String WEBLOGIC_CHECK_ERROR_LOG_CHANNEL = "weblogic_security_check_error";
    private static final String BOOT_PROPERTIES_PATH = "servers" + File.separator + "AdminServer" + File.separator + "security" + File.separator + "boot.properties";
    private static final String[] WEAK_WORDS = new String[]{"weblogic", "weblogic1", "weblogic123", "admin", "123456"};
    private static final Logger LOGGER = Logger.getLogger(HookHandler.class.getName());

    @Override
    public List<EventInfo> checkParam(CheckParameter checkParameter) {
        String domainPath = System.getProperty("user.dir");
        List<EventInfo> infos = new LinkedList<EventInfo>();
        checkManagerPassword(domainPath, infos);
        return infos;
    }

    private void checkManagerPassword(String domainPath, List<EventInfo> infos) {
        File bootProperties = new File(domainPath + File.separator + BOOT_PROPERTIES_PATH);
        if (!(bootProperties.exists() && bootProperties.canRead())) {
            LOGGER.warn(WEBLOGIC_CHECK_ERROR_LOG_CHANNEL + ": can not load file " + BOOT_PROPERTIES_PATH);
        }
        String encryptedPassword = getProperties(bootProperties, "password");
        String decryptedPassword = decrypt(encryptedPassword, domainPath);
        List<String> checkList = Arrays.asList(WEAK_WORDS);
        if (checkList.contains(decryptedPassword)) {
            infos.add(new SecurityPolicyInfo(SecurityPolicyInfo.Type.MANAGER_PASSWORD, "Weblogic security baseline - detected weak password combination in " + BOOT_PROPERTIES_PATH, true));
        }
    }

    private String getProperties(File file, String keyWord) {
        Properties prop = new Properties();
        String value = null;
        try {
            InputStream InputStream = new BufferedInputStream(new FileInputStream(file));
            prop.load(InputStream);
            value = prop.getProperty(keyWord);
        } catch (Exception e) {
            LOGGER.warn(WEBLOGIC_CHECK_ERROR_LOG_CHANNEL + ": can not find " + keyWord);
        }
        return value != null ? value : "";
    }

    private String decrypt(String decrypted, String path) {
        String decryptedString = null;
        try {
            ClassLoader classLoader = WeblogicSecurityChecker.class.getClassLoader();
            Object encryptionService = Reflection.invokeStaticMethod("weblogic.security.internal.SerializedSystemIni", "getEncryptionService", new Class[]{String.class}, path);
            if (encryptionService != null) {
                Object clearOrEncryptedService = classLoader.loadClass("weblogic.security.internal.encryption.ClearOrEncryptedService").getDeclaredConstructor(classLoader.loadClass("weblogic.security.internal.encryption.EncryptionService")).newInstance(encryptionService);
                decryptedString = Reflection.invokeStringMethod(clearOrEncryptedService, "decrypt", new Class[]{String.class}, decrypted);
            }
        } catch (Exception e) {
            LOGGER.warn(WEBLOGIC_CHECK_ERROR_LOG_CHANNEL + ": can not decrypt the encryptedString");
        }
        return decryptedString != null ? decryptedString : "";
    }
}
