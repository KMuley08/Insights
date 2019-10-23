package com.cognizant.devops.platforminsightswebhook.test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import com.cognizant.devops.platformcommons.config.ApplicationConfigCache;
import com.cognizant.devops.platformcommons.config.ApplicationConfigProvider;
import com.cognizant.devops.platformcommons.config.MessageQueueDataModel;
import com.cognizant.devops.platforminsightswebhook.config.WebHookMessagePublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class PublishEventTest {
	
  String message = "{  \"action\": \"created\",  \"comment\": {  \"url\": \"https://api.github.com/repos/Codertocat/Hello-World/comments/33548674\",  \"html_url\": \"https://github.com/Codertocat/Hello-World/commit/6113728f27ae82c7b1a177c8d03f9e96e0adf246#commitcomment-33548674\",  \"id\": 33548674,  \"node_id\": \"MDEzOkNvbW1pdENvbW1lbnQzMzU0ODY3NA==\",  \"user\": { \"login\": \"Codertocat\", \"id\": 21031067, \"node_id\": \"MDQ6VXNlcjIxMDMxMDY3\", \"avatar_url\": \"https://avatars1.githubusercontent.com/u/21031067?v=4\", \"gravatar_id\": \"\", \"url\": \"https://api.github.com/users/Codertocat\", \"html_url\": \"https://github.com/Codertocat\", \"followers_url\": \"https://api.github.com/users/Codertocat/followers\", \"following_url\": \"https://api.github.com/users/Codertocat/following{/other_user}\", \"gists_url\": \"https://api.github.com/users/Codertocat/gists{/gist_id}\", \"starred_url\": \"https://api.github.com/users/Codertocat/starred{/owner}{/repo}\", \"subscriptions_url\": \"https://api.github.com/users/Codertocat/subscriptions\", \"organizations_url\": \"https://api.github.com/users/Codertocat/orgs\", \"repos_url\": \"https://api.github.com/users/Codertocat/repos\", \"events_url\": \"https://api.github.com/users/Codertocat/events{/privacy}\", \"received_events_url\": \"https://api.github.com/users/Codertocat/received_events\", \"type\": \"User\", \"site_admin\": false  },  \"position\": null,  \"line\": null,  \"path\": null,  \"commit_id\": \"6113728f27ae82c7b1a177c8d03f9e96e0adf246\",  \"created_at\": \"2019-05-15T15:20:39Z\",  \"updated_at\": \"2019-05-15T15:20:39Z\",  \"author_association\": \"OWNER\",  \"body\": \"This is a really good change! :+1:\"  },  \"repository\": {  \"id\": 186853002,  \"node_id\": \"MDEwOlJlcG9zaXRvcnkxODY4NTMwMDI=\",  \"name\": \"Hello-World\",  \"full_name\": \"Codertocat/Hello-World\",  \"private\": false,  \"owner\": { \"login\": \"Codertocat\", \"id\": 21031067, \"node_id\": \"MDQ6VXNlcjIxMDMxMDY3\", \"avatar_url\": \"https://avatars1.githubusercontent.com/u/21031067?v=4\", \"gravatar_id\": \"\", \"url\": \"https://api.github.com/users/Codertocat\", \"html_url\": \"https://github.com/Codertocat\", \"followers_url\": \"https://api.github.com/users/Codertocat/followers\", \"following_url\": \"https://api.github.com/users/Codertocat/following{/other_user}\", \"gists_url\": \"https://api.github.com/users/Codertocat/gists{/gist_id}\", \"starred_url\": \"https://api.github.com/users/Codertocat/starred{/owner}{/repo}\", \"subscriptions_url\": \"https://api.github.com/users/Codertocat/subscriptions\", \"organizations_url\": \"https://api.github.com/users/Codertocat/orgs\", \"repos_url\": \"https://api.github.com/users/Codertocat/repos\", \"events_url\": \"https://api.github.com/users/Codertocat/events{/privacy}\", \"received_events_url\": \"https://api.github.com/users/Codertocat/received_events\", \"type\": \"User\", \"site_admin\": false  },  \"html_url\": \"https://github.com/Codertocat/Hello-World\",  \"description\": null,  \"fork\": false,  \"url\": \"https://api.github.com/repos/Codertocat/Hello-World\",  \"forks_url\": \"https://api.github.com/repos/Codertocat/Hello-World/forks\",  \"keys_url\": \"https://api.github.com/repos/Codertocat/Hello-World/keys{/key_id}\",  \"collaborators_url\": \"https://api.github.com/repos/Codertocat/Hello-World/collaborators{/collaborator}\",  \"teams_url\": \"https://api.github.com/repos/Codertocat/Hello-World/teams\",  \"hooks_url\": \"https://api.github.com/repos/Codertocat/Hello-World/hooks\",  \"issue_events_url\": \"https://api.github.com/repos/Codertocat/Hello-World/issues/events{/number}\",  \"events_url\": \"https://api.github.com/repos/Codertocat/Hello-World/events\",  \"assignees_url\": \"https://api.github.com/repos/Codertocat/Hello-World/assignees{/user}\",  \"branches_url\": \"https://api.github.com/repos/Codertocat/Hello-World/branches{/branch}\",  \"tags_url\": \"https://api.github.com/repos/Codertocat/Hello-World/tags\",  \"blobs_url\": \"https://api.github.com/repos/Codertocat/Hello-World/git/blobs{/sha}\",  \"git_tags_url\": \"https://api.github.com/repos/Codertocat/Hello-World/git/tags{/sha}\",  \"git_refs_url\": \"https://api.github.com/repos/Codertocat/Hello-World/git/refs{/sha}\",  \"trees_url\": \"https://api.github.com/repos/Codertocat/Hello-World/git/trees{/sha}\",  \"statuses_url\": \"https://api.github.com/repos/Codertocat/Hello-World/statuses/{sha}\",  \"languages_url\": \"https://api.github.com/repos/Codertocat/Hello-World/languages\",  \"stargazers_url\": \"https://api.github.com/repos/Codertocat/Hello-World/stargazers\",  \"contributors_url\": \"https://api.github.com/repos/Codertocat/Hello-World/contributors\",  \"subscribers_url\": \"https://api.github.com/repos/Codertocat/Hello-World/subscribers\",  \"subscription_url\": \"https://api.github.com/repos/Codertocat/Hello-World/subscription\",  \"commits_url\": \"https://api.github.com/repos/Codertocat/Hello-World/commits{/sha}\",  \"git_commits_url\": \"https://api.github.com/repos/Codertocat/Hello-World/git/commits{/sha}\",  \"comments_url\": \"https://api.github.com/repos/Codertocat/Hello-World/comments{/number}\",  \"issue_comment_url\": \"https://api.github.com/repos/Codertocat/Hello-World/issues/comments{/number}\",  \"contents_url\": \"https://api.github.com/repos/Codertocat/Hello-World/contents/{+path}\",  \"compare_url\": \"https://api.github.com/repos/Codertocat/Hello-World/compare/{base}...{head}\",  \"merges_url\": \"https://api.github.com/repos/Codertocat/Hello-World/merges\",  \"archive_url\": \"https://api.github.com/repos/Codertocat/Hello-World/{archive_format}{/ref}\",  \"downloads_url\": \"https://api.github.com/repos/Codertocat/Hello-World/downloads\",  \"issues_url\": \"https://api.github.com/repos/Codertocat/Hello-World/issues{/number}\",  \"pulls_url\": \"https://api.github.com/repos/Codertocat/Hello-World/pulls{/number}\",  \"milestones_url\": \"https://api.github.com/repos/Codertocat/Hello-World/milestones{/number}\",  \"notifications_url\": \"https://api.github.com/repos/Codertocat/Hello-World/notifications{?since,all,participating}\",  \"labels_url\": \"https://api.github.com/repos/Codertocat/Hello-World/labels{/name}\",  \"releases_url\": \"https://api.github.com/repos/Codertocat/Hello-World/releases{/id}\",  \"deployments_url\": \"https://api.github.com/repos/Codertocat/Hello-World/deployments\",  \"created_at\": \"2019-05-15T15:19:25Z\",  \"updated_at\": \"2019-05-15T15:20:34Z\",  \"pushed_at\": \"2019-05-15T15:20:33Z\",  \"git_url\": \"git://github.com/Codertocat/Hello-World.git\",  \"ssh_url\": \"git@github.com:Codertocat/Hello-World.git\",  \"clone_url\": \"https://github.com/Codertocat/Hello-World.git\",  \"svn_url\": \"https://github.com/Codertocat/Hello-World\",  \"homepage\": null,  \"size\": 0,  \"stargazers_count\": 0,  \"watchers_count\": 0,  \"language\": \"Ruby\",  \"has_issues\": true,  \"has_projects\": true,  \"has_downloads\": true,  \"has_wiki\": true,  \"has_pages\": true,  \"forks_count\": 0,  \"mirror_url\": null,  \"archived\": false,  \"disabled\": false,  \"open_issues_count\": 2,  \"license\": null,  \"forks\": 0,  \"open_issues\": 2,  \"watchers\": 0,  \"default_branch\": \"master\"  },  \"sender\": {  \"login\": \"Codertocat\",  \"id\": 21031067,  \"node_id\": \"MDQ6VXNlcjIxMDMxMDY3\",  \"avatar_url\": \"https://avatars1.githubusercontent.com/u/21031067?v=4\",  \"gravatar_id\": \"\",  \"url\": \"https://api.github.com/users/Codertocat\",  \"html_url\": \"https://github.com/Codertocat\",  \"followers_url\": \"https://api.github.com/users/Codertocat/followers\",  \"following_url\": \"https://api.github.com/users/Codertocat/following{/other_user}\",  \"gists_url\": \"https://api.github.com/users/Codertocat/gists{/gist_id}\",  \"starred_url\": \"https://api.github.com/users/Codertocat/starred{/owner}{/repo}\",  \"subscriptions_url\": \"https://api.github.com/users/Codertocat/subscriptions\",  \"organizations_url\": \"https://api.github.com/users/Codertocat/orgs\",  \"repos_url\": \"https://api.github.com/users/Codertocat/repos\",  \"events_url\": \"https://api.github.com/users/Codertocat/events{/privacy}\",  \"received_events_url\": \"https://api.github.com/users/Codertocat/received_events\",  \"type\": \"User\",  \"site_admin\": false  }  }";

  WebHookMessagePublisher webHookMessagePublisher;
  
  @BeforeTest
  public void init()
  {
	ApplicationConfigCache.loadConfigCache();
	webHookMessagePublisher = new WebHookMessagePublisher();
	webHookMessagePublisher.initilizeMq();   	
  }  
  
  @Test
  public void testPublishEvent() throws TimeoutException, IOException {
	    	 
   webHookMessagePublisher.publishEventAction(message.getBytes(),"IPW_WebhookTest");
   
  }
  
  @AfterTest
  public void cleanUp() throws IOException, TimeoutException
  {
	    ConnectionFactory factory = new ConnectionFactory();
		MessageQueueDataModel messageQueueConfig = ApplicationConfigProvider.getInstance().getMessageQueue();
		factory.setHost(messageQueueConfig.getHost());
		factory.setUsername(messageQueueConfig.getUser());
		factory.setPassword(messageQueueConfig.getPassword());
		Connection connection = null;
		Channel channel = null;
		connection = factory.newConnection();
		channel = connection.createChannel();
		channel.queuePurge("IPW_WebhookTest");
		connection.close();		
  }
}
