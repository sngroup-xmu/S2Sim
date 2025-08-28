package org.batfish.diagnosis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class InputData {

    public static String getStr(File jsonFile){
        String jsonStr = "";
        try {
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String concatFilePath(String rootPath, String sub) {
        if (sub!=null && !sub.equals("")) {
            return rootPath + "/" + sub;
        } else {
            return rootPath;
        }
    }

    public static String findFilePathWithMatchedName(String rootPath, String matchedName) {
        if (rootPath==null) {
            int a =0;
        }
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) {
            System.out.println("ERROR!!!!ERROR!!!!!");
            System.out.println(rootPath);
            System.out.println(matchedName);
            assert false;
        }
        File[] files = rootDir.listFiles();
        for (File file: files) {
            String aa = file.getName();
            if (file.getName().toLowerCase().contains(matchedName.toLowerCase())) {
                return file.getAbsolutePath();
            }
        }
        return "";
    }

    public static String projectRootPath = System.getProperty("user.dir");

    public InputData() {}

    public static String filterInvalidFilePath(String filePath) {
        File file = new File(filePath);
        assert (file.exists());
        return filePath;

    }


    /**
     * 得到修复后配置的根路径，每个路径下会存放【修改设备（增量）的配置文件】和【result.txt文件用于说明所有设备的配置改动】
     *
     * @return {@link String}
     */// 待读取的目录
    public static String getRepairedCfgRootPath(String oldConfigRootPath) {
        return concatFilePath(oldConfigRootPath, "patch");
    }


}
