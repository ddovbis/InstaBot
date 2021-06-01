# InstaBot 

InstaBot helps to automatize some of the key processes of interacting with Instagram.

#### Disclaimer
This project is created for training purposes. 
The main goal of it is practicing the technologies involved.
As of writting this disclaimer, using bots is prohibited by Instagram's policy.
Shall you consider to use the bot for yourself, please consider that you do so at your own risk.

#### Key features
- Extract Instagram users that are "followed by", or "follow" the application user (or otherwise - related users).
- Create a Google sheet with extracted data and send it by email. The sheet contains all related users split into following categories: "Is not following you back", "You follow each other", "You are not following back".
- Iterate through Instagram pages of related users that are in the "Is not following you back" category. For each user the bot will pick 2-4 posts and will "like" them. All interactions with related users are added to the database. The bot will calculate how many interactions have been performed in the last 1h and 24h, and will stop if the maximum hourly or daily interactions limit is reached.

#### Used technologies/libraries:
- Maven
- Groovy 3 (using mostly Java syntax)
- Spring Boot
- Spring Data JPA / Hibernate / H2
- Selenium WebDriver - for web-scraping and interactions with the browser on behalf of the application user
- Jsoup - for parsing HTML data
- Log4j2

#### Requirements:
- Google Chrome browser - latest offical version
- Java 8

#### Usage
// TBA 

#### Config
// TBA