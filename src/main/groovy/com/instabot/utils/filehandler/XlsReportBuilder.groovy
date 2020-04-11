package com.instabot.utils.filehandler

import com.instabot.data.model.user.User
import com.instabot.data.services.user.UserDataService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class XlsReportBuilder {
    public static final String FOLLOWED_NOT_FOLLOWERS_SHEET_NAME = "Is not following you back"
    public static final String FOLLOWED_FOLLOWERS_SHEET_NAME = "You follow each other"
    public static final String NOT_FOLLOWED_FOLLOWERS_SHEET_NAME = "You are not following back"

    private static final Logger LOG = LogManager.getLogger(XlsReportBuilder.class)

    @Autowired
    private UserDataService userDataService

    Workbook getRelatedUsersReport(String masterUsername) {
        LOG.info("Create XLS document with related users report")
        List<User> allRelatedUsers = userDataService.getAllByMasterUsername(masterUsername)

        // Creating Workbook instances
        Workbook workbook = new HSSFWorkbook()

        // add sheets with different related users categories
        addSheetWithRelatedUsers(workbook, allRelatedUsers, FOLLOWED_NOT_FOLLOWERS_SHEET_NAME, { User user -> !user.isFollower && user.isFollowed })
        addSheetWithRelatedUsers(workbook, allRelatedUsers, FOLLOWED_FOLLOWERS_SHEET_NAME, { User user -> user.isFollower && user.isFollowed })
        addSheetWithRelatedUsers(workbook, allRelatedUsers, NOT_FOLLOWED_FOLLOWERS_SHEET_NAME, { User user -> user.isFollower && !user.isFollowed })

        LOG.info("XLS document successfully created")
        return workbook
    }


    private void addSheetWithRelatedUsers(Workbook workbook, List<User> allRelatedUsers, String sheetName, def condition) {
        Sheet sheet = workbook.createSheet(sheetName)

        // create header
        int rowIndex = 0
        Row row = sheet.createRow(rowIndex++)
        row.createCell(0).setCellValue("USERNAME")
        row.createCell(1).setCellValue("NAME")

        // add all users satisfying condition
        allRelatedUsers.each { user ->
            if (condition(user)) {
                row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(user.username)
                row.createCell(1).setCellValue(user.name)
            }
        }

        sheet.autoSizeColumn(0)
        sheet.autoSizeColumn(1)

        LOG.info("$sheetName rows: $rowIndex")
    }
}
