package com.instabot.utils.argshandler

import org.apache.commons.cli.*

class ArgsHandler {
    private static final OPTIONS = new Options()

    /**
     * Initialize options
     */
    static {
        Option help = new Option("h", "help", false, "print usage")
        OPTIONS.addOption(help)

        Option instaBotConfPath = new Option("i", "insta-bot-conf-path", true, "insta-bot confirguration file path")
        instaBotConfPath.setRequired(true)
        OPTIONS.addOption(instaBotConfPath)
    }

    static void process(String[] args) {
        CommandLineParser parser = new DefaultParser()
        CommandLine cmd

        try {
            cmd = parser.parse(OPTIONS, args)
        } catch (ParseException e) {
            println e.getMessage()
            printUsageAndExit()
        }

        if (cmd.getOptionValue("help")) {
            printUsageAndExit()
        }

        String instaBotConfPath = cmd.getOptionValue("insta-bot-conf-path")
        if (instaBotConfPath == null) {
            println "Please provide insta-bot configuration file path"
            printUsageAndExit()
        }

        System.properties.setProperty("instabot.conf.path", instaBotConfPath)
    }

    private static void printUsageAndExit() {
        HelpFormatter formatter = new HelpFormatter()
        formatter.printHelp("insta-bot", OPTIONS)
        System.exit(1)
    }
}
