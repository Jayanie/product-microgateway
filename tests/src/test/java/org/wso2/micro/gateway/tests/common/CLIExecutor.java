/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.micro.gateway.tests.common;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.apimgt.gateway.cli.constants.GatewayCliConstants;
import org.wso2.micro.gateway.tests.context.Constants;
import org.wso2.micro.gateway.tests.context.ServerLogReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Execute APIM CLI functions.
 */
public class CLIExecutor {
    private static final Logger log = LoggerFactory.getLogger(CLIExecutor.class);
    private String homeDirectory;
    private String cliHome;
    private static CLIExecutor instance;

    public void generate(String label, String project) throws Exception {

        String baseDir = (System.getProperty(Constants.SYSTEM_PROP_BASE_DIR, ".")) + File.separator + "target";
        Path path = Files.createTempDirectory(new File(baseDir).toPath(), "userProject");
        log.info("CLI Project Home: " + path.toString());

        System.setProperty(GatewayCliConstants.CLI_HOME, this.cliHome);
        log.info("CLI Home: " + this.cliHome);

        String config = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("confs" + File.separator +
                        "default-cli-test-config.toml")).getPath()).getAbsolutePath();
        System.setProperty("user.dir", path.toString());

        String mgwCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_BIN + File.separator + "micro-gw";
        homeDirectory = path.toString();

        String[] initCmdArray = {"bash", mgwCommand, "init", project, "-f"};
        Process initProcess = Runtime.getRuntime().exec(initCmdArray, null, new File(homeDirectory));
        new ServerLogReader("errorStream", initProcess.getErrorStream()).start();
        new ServerLogReader("inputStream", initProcess.getInputStream()).start();
        int initExitCode = initProcess.waitFor();
        if (initExitCode != 0) {
            throw new RuntimeException("Error occurred when intializing.");
        }

        String[] importCmdArray = {"bash", mgwCommand, "import", project, "--label", label, "--username", "admin",
                "--password", "admin", "--server-url", "http://localhost:9443", "--truststore",
                "lib/platform/bre/security/ballerinaTruststore.p12", "--truststore-pass", "ballerina", "--config",
                config};

        Process importProcess = Runtime.getRuntime().exec(importCmdArray, null, new File(homeDirectory));
        new ServerLogReader("errorStream", importProcess.getErrorStream()).start();
        new ServerLogReader("inputStream", importProcess.getInputStream()).start();
        int importExitCode = importProcess.waitFor();
        if (importExitCode != 0) {
            throw new RuntimeException("Error occurred when importing.");
        }

        File policyYamlResource = new File(Objects.requireNonNull(getClass().getClassLoader()
                .getResource("policies.yaml")).getPath());
        String apiDefinitionPath = path + File.separator + project + File.separator;
        File policyYamlFile = new File(apiDefinitionPath + "/policies.yaml");
        FileUtils.copyFile(policyYamlResource, policyYamlFile);

        String[] cmdArray = new String[]{"bash", mgwCommand, "build", project};
        Process process = Runtime.getRuntime().exec(cmdArray, null, new File(homeDirectory));

        new ServerLogReader("errorStream", process.getErrorStream()).start();
        new ServerLogReader("inputStream", process.getInputStream()).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error occurred when building.");
        }
    }

    public void generateFromDefinition( String project, String[] openAPIFileNames)
            throws Exception {

        String baseDir = (System.getProperty(Constants.SYSTEM_PROP_BASE_DIR, ".")) + File.separator + "target";
        Path path = Files.createTempDirectory(new File(baseDir).toPath(), "userProject");
        log.info("CLI Project Home: " + path.toString());

        System.setProperty(GatewayCliConstants.CLI_HOME, this.cliHome);
        log.info("CLI Home: " + this.cliHome);
        String mgwCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_BIN + File.separator + "micro-gw";
        homeDirectory = path.toString();

        String[] initCmdArray = {"bash", mgwCommand, "init", project, "-f"};
        Process initProcess = Runtime.getRuntime().exec(initCmdArray, null, new File(homeDirectory));
        new ServerLogReader("errorStream", initProcess.getErrorStream()).start();
        new ServerLogReader("inputStream", initProcess.getInputStream()).start();
        int initExitCode = initProcess.waitFor();
        if (initExitCode != 0) {
            throw new RuntimeException("Error occurred when building.");
        }
        
        String apiDefinitionPath = path + File.separator + project + File.separator;
        for(String openAPIFileName : openAPIFileNames) {

            File swaggerFilePath = new File(
                    getClass().getClassLoader().getResource(Constants.OPEN_APIS + File.separator +
                            openAPIFileName).getPath());

            File swaggerDesPath = new File(
                    path + File.separator + project + File.separator +
                            GatewayCliConstants.PROJECT_API_DEFINITIONS_DIR + File.separator + openAPIFileName
                            .substring(openAPIFileName.lastIndexOf(File.separator) + 1));

            FileUtils.copyFile(swaggerFilePath, swaggerDesPath);
        }
        File policyYamlResource = new File(getClass().getClassLoader().getResource("policies.yaml").getPath());
        File policyYamlFile = new File(apiDefinitionPath + "/policies.yaml");
        FileUtils.copyFile(policyYamlResource, policyYamlFile);

        String[] buildCmdArray = new String[]{"bash", mgwCommand, "build", project};
        Process process = Runtime.getRuntime().exec(buildCmdArray, null, new File(homeDirectory));
        new ServerLogReader("errorStream", process.getErrorStream()).start();
        new ServerLogReader("inputStream", process.getInputStream()).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error occurred when building.");
        }
    }

    public void generatePassingFlag(String label, String project, String additionalFlag) throws Exception {

        String baseDir = (System.getProperty(Constants.SYSTEM_PROP_BASE_DIR, ".")) + File.separator + "target";
        Path path = Files.createTempDirectory(new File(baseDir).toPath(), "userProject");
        log.info("CLI Project Home: " + path.toString());

        System.setProperty(GatewayCliConstants.CLI_HOME, this.cliHome);
        log.info("CLI Home: " + this.cliHome);

        String config = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("confs" + File.separator +
                        "default-cli-test-config.toml")).getPath()).getAbsolutePath();
        System.setProperty("user.dir", path.toString());

        String mgwCommand = this.cliHome + File.separator + GatewayCliConstants.CLI_BIN + File.separator + "micro-gw";
        homeDirectory = path.toString();

        String[] initCmdArray = {"bash", mgwCommand, "init", project, "-f"};
        Process initProcess = Runtime.getRuntime().exec(initCmdArray, null, new File(homeDirectory));
        new ServerLogReader("errorStream", initProcess.getErrorStream()).start();
        new ServerLogReader("inputStream", initProcess.getInputStream()).start();
        int initExitCode = initProcess.waitFor();
        if (initExitCode != 0) {
            throw new RuntimeException("Error occurred when intializing.");
        }

        String[] importCmdArray = {"bash", mgwCommand, "import", project, "--label", label, "--username", "admin",
                "--password", "admin", "--server-url", "http://localhost:9443", "--truststore",
                "lib/platform/bre/security/ballerinaTruststore.p12", "--truststore-pass", "ballerina", "--config",
                config, additionalFlag};
        Process importProcess = Runtime.getRuntime().exec(importCmdArray, null, new File(homeDirectory));
        new ServerLogReader("errorStream", importProcess.getErrorStream()).start();
        new ServerLogReader("inputStream", importProcess.getInputStream()).start();
        int importExitCode = importProcess.waitFor();
        if (importExitCode != 0) {
            throw new RuntimeException("Error occurred when importing.");
        }


        String[] cmdArray = new String[]{"bash", mgwCommand, "build", project};
        Process process = Runtime.getRuntime().exec(cmdArray, null, new File(homeDirectory));

        new ServerLogReader("errorStream", process.getErrorStream()).start();
        new ServerLogReader("inputStream", process.getInputStream()).start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error occurred when building.");
        }
    }

    private CLIExecutor() {
    }

    public static CLIExecutor getInstance() {
        if (instance == null) {
            instance = new CLIExecutor();
        }
        return instance;
    }

    public void setCliHome(String cliHome) {
        this.cliHome = cliHome;
    }

    public String getLabelBalx(String project) {
        return homeDirectory + File.separator + project + File.separator + "target" + File.separator + project +
                ".balx";
    }
}