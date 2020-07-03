/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.nacos.config.server.service;

import com.alibaba.nacos.common.utils.IoUtils;
import com.alibaba.nacos.config.server.utils.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static com.alibaba.nacos.config.server.utils.LogUtil.fatalLog;

/**
 * Switch
 * 开关类
 * @author Nacos
 */
@Service
public class SwitchService {
    public static final String SWITCH_META_DATAID = "com.alibaba.nacos.meta.switch";

    public static final String FIXED_POLLING = "isFixedPolling";
    public static final String FIXED_POLLING_INTERVAL = "fixedPollingInertval";

    public static final String FIXED_DELAY_TIME = "fixedDelayTime";

    public static final String DISABLE_APP_COLLECTOR = "disableAppCollector";

    private static volatile Map<String, String> switches = new HashMap<String, String>();
    /**获取指定开关的配置，如果未设置，则直接返回defaultValue*/
    public static boolean getSwitchBoolean(String key, boolean defaultValue) {
        boolean rtn = defaultValue;
        try {
            String value = switches.get(key);
            rtn = value != null ? Boolean.parseBoolean(value) : defaultValue;
        } catch (Exception e) {
            rtn = defaultValue;
            LogUtil.fatalLog.error("corrupt switch value {}={}", key, switches.get(key));
        }
        return rtn;
    }

    public static int getSwitchInteger(String key, int defaultValue) {
        int rtn = defaultValue;
        try {
            String status = switches.get(key);
            rtn = status != null ? Integer.parseInt(status) : defaultValue;
        } catch (Exception e) {
            rtn = defaultValue;
            LogUtil.fatalLog.error("corrupt switch value {}={}", key, switches.get(key));
        }
        return rtn;
    }

    public static String getSwitchString(String key, String defaultValue) {
        String value = switches.get(key);
        return StringUtils.isBlank(value) ? defaultValue : value;
    }
    /**加载配置，储存各种的开关配置  传入的config应该是一个多行的数据*/
    public static void load(String config) {
        //如果config为空，则表示没有设置配置，无需加载，直接记录异常，终止执行
        if (StringUtils.isBlank(config)) {
            fatalLog.error("switch config is blank.");
            return;
        }
        fatalLog.warn("[switch-config] {}", config);

        Map<String, String> map = new HashMap<String, String>(30);
        try {
            //遍历所有的配置（一行一行遍历）
            for (String line : IoUtils.readLines(new StringReader(config))) {
                //如果此行配置不是空，并且不是以#号开头（有效配置，因为#号开头的是被注释掉的）
                if (!StringUtils.isBlank(line) && !line.startsWith("#")) {
                    //根据等号进行切分出key和value
                    String[] array = line.split("=");
                    //如果配置有问题（不是a=b这种类型格式均是无效的），则进行报警，并忽略此条继续处理后面
                    if (array == null || array.length != 2) {
                        LogUtil.fatalLog.error("corrupt switch record {}", line);
                        continue;
                    }
                    //将此条配置加入到map缓存中
                    String key = array[0].trim();
                    String value = array[1].trim();

                    map.put(key, value);
                }
                //将map设置到switches中
                switches = map;
                fatalLog.warn("[reload-switches] {}", getSwitches());
            }
        } catch (IOException e) {
            LogUtil.fatalLog.warn("[reload-switches] error! {}", config);
        }
    }

    public static String getSwitches() {
        StringBuilder sb = new StringBuilder();

        String split = "";
        for (Map.Entry<String, String> entry : switches.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(split);
            sb.append(key);
            sb.append("=");
            sb.append(value);
            split = "; ";
        }

        return sb.toString();
    }

}
