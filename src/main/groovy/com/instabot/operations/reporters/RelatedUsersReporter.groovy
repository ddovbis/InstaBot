package com.instabot.operations.reporters

import com.instabot.config.InstaBotConfig
import com.instabot.utils.filehandler.XlsReportBuilder
import com.instabot.utils.mail.MailSender
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.poi.ss.usermodel.Workbook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

import java.time.LocalDateTime

@Component
class RelatedUsersReporter {
    private final Logger LOG = LogManager.getLogger(RelatedUsersReporter.class)

    @Autowired
    InstaBotConfig initializeInstaBotConfig
    @Autowired
    MailSender mailSender
    @Autowired
    XlsReportBuilder xlsReportBuilder

    boolean sendReport

    @Bean("initializeRelatedUsersReporter")
    @DependsOn("initializeInstaBotConfig")
    private void initialize() {
        LOG.info("Initialize RelatedUsersReporter")
        sendReport = initializeInstaBotConfig.getIniFile().get("related-users", "send-report", Boolean.class)
    }

    void sendReport(String masterUsername) {
        if (!sendReport) {
            LOG.debug("Report sending is desabled; no report will be sent for related users")
            return
        } else {
            LOG.info("Compute and send related users report")
        }

        Workbook relatedUsersReport = xlsReportBuilder.getRelatedUsersReport(masterUsername)

        String text = computeText(relatedUsersReport)
        File tempReportFile = getReportFile(relatedUsersReport, masterUsername)
        mailSender.send("[InstaBot] Related users report: $masterUsername", text, tempReportFile)
        tempReportFile.delete()
    }

    private computeText(Workbook relatedUsersReport) {
        return "" +
                "$XlsReportBuilder.FOLLOWED_NOT_FOLLOWERS_SHEET_NAME: ${relatedUsersReport.getSheet(XlsReportBuilder.FOLLOWED_NOT_FOLLOWERS_SHEET_NAME).getLastRowNum()}\n" +
                "$XlsReportBuilder.FOLLOWED_FOLLOWERS_SHEET_NAME: ${relatedUsersReport.getSheet(XlsReportBuilder.FOLLOWED_FOLLOWERS_SHEET_NAME).getLastRowNum()}\n" +
                "$XlsReportBuilder.NOT_FOLLOWED_FOLLOWERS_SHEET_NAME: ${relatedUsersReport.getSheet(XlsReportBuilder.NOT_FOLLOWED_FOLLOWERS_SHEET_NAME).getLastRowNum()}\n" +
                "Please find the detailed report attached."
    }

    private getReportFile(Workbook relatedUsersReport, String masterUsername) {
        File file = new File(getXlsFileName(masterUsername))
        file.getParentFile().mkdirs()
        relatedUsersReport.write(file)
        return file
    }

    private String getXlsFileName(String masterUsername) {
        return "data/tmp/${masterUsername}_related_users-report_${LocalDateTime.now().format("yyyyMMddHHmm")}.xls"
    }
}
