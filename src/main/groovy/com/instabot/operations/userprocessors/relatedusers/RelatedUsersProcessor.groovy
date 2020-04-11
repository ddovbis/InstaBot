package com.instabot.operations.userprocessors.relatedusers

import com.instabot.data.model.user.User
import com.instabot.data.services.user.UserDataService
import com.instabot.operations.helper.OperationsHelper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RelatedUsersProcessor {
    private static final Logger LOG = LogManager.getLogger(RelatedUsersProcessor.class)

    @Autowired
    UserDataService userDataService
    @Autowired
    RelatedUsersLiker relatedUsersLiker
    @Autowired
    OperationsHelper operationsHelper

    void processRelatedUsers(String masterUsername) {
        LOG.info("Start processing users related to master user: $masterUsername")

        // TODO uncomment after RelatedUsersCommenter is implemented
        // List<User> usersToBeProcessed = userDataService.getAllToBeProcessedByMasterUsername(masterUsername)
        List<User> usersToBeProcessed = userDataService.getAllToBeLikedByMasterUsername(masterUsername)

        for (User userToBeProcessed : usersToBeProcessed) {
            relatedUsersLiker.likeUserPosts(userToBeProcessed)

            // TODO Add do-not-process-followers .ini parameter; if enabled set the user to LIKED and COMMENTED

            // TODO Implement waiting time based on .ini + lockedUntil variable
            sleep(operationsHelper.getRandomInt(18, 36) * 1000)

            // comment the newest user post here

            // TODO if nr. of likes >= target and not to be commented or nr. of comments >= target -> set user to processed
            //   for now; set min max nr. of comments to -1, and set all to commented
        }
    }
}
