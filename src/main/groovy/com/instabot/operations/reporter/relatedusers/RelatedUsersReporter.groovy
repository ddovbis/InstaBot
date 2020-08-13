package com.instabot.operations.reporter.relatedusers

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

import java.text.SimpleDateFormat

@Component
class RelatedUsersReporter {
    private final Logger LOG = LogManager.getLogger(RelatedUsersReporter.class)

    @Autowired
    private InstaBotConfig initializeInstaBotConfig
    @Autowired
    private MailSender mailSender
    @Autowired
    private XlsReportBuilder xlsReportBuilder

    private boolean sendReport
    private String masterUsername

    @Bean("initializeRelatedUsersReporter")
    @DependsOn("initializeInstaBotConfig")
    private void initialize() {
        LOG.info("Initialize RelatedUsersReporter")
        sendReport = initializeInstaBotConfig.getIniFile().get("related-users", "send-report", Boolean.class)
        masterUsername = initializeInstaBotConfig.getIniFile().get("related-users", "master-username", String.class)
    }

    void sendReport() {
        if (!sendReport) {
            LOG.debug("Report sending is disabled; no report will be sent for related users")
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
        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmm").format(new Date())
        return "data/tmp/${masterUsername}_related_users-report_${currentDateTime}.xls"
    }
}
