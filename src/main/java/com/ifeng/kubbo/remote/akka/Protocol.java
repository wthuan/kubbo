package com.ifeng.kubbo.remote.akka;

import java.io.Serializable;
import java.util.Set;

/**
 * <title>Protocol</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-1
 */
public class Protocol {

    public static class Register implements Serializable {

        private Set<ProviderConfig> providerConfigs;

        public Register(Set<ProviderConfig> providerConfigs) {
            this.providerConfigs = providerConfigs;
        }

        public Set<ProviderConfig> getProviderConfigs() {
            return this.providerConfigs;
        }
    }

    public static class UnRegister implements Serializable{
        private Set<ProviderConfig> providerConfigs;

        public UnRegister(Set<ProviderConfig> providerConfigs){
            this.providerConfigs = providerConfigs;
        }

        public Set<ProviderConfig> getProviderConfigs(){
            return this.providerConfigs;
        }
    }
}