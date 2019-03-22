package org.sagebionetworks.web.client.presenter;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.PaginatedTeamIds;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasResponse;
import org.sagebionetworks.web.client.ClientProperties;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GWTWrapper;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.place.Synapse.ProfileArea;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.view.ProfileView;
import org.sagebionetworks.web.client.widget.LoadMoreWidgetContainer;
import org.sagebionetworks.web.client.widget.entity.ChallengeBadge;
import org.sagebionetworks.web.client.widget.entity.ProjectBadge;
import org.sagebionetworks.web.client.widget.entity.PromptModalView;
import org.sagebionetworks.web.client.widget.entity.browse.EntityBrowserUtils;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.client.widget.entity.file.downloadlist.DownloadListWidget;
import org.sagebionetworks.web.client.widget.profile.UserProfileModalWidget;
import org.sagebionetworks.web.client.widget.team.OpenTeamInvitationsWidget;
import org.sagebionetworks.web.client.widget.team.TeamListWidget;
import org.sagebionetworks.web.shared.exceptions.ConflictException;

import com.google.common.util.concurrent.FutureCallback;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ProfilePresenter extends AbstractActivity implements ProfileView.Presenter, Presenter<Profile> {
	public static final int DELAY_GET_MY_TEAMS = 300;
	public static final String USER_PROFILE_VERIFICATION_VISIBLE_STATE_KEY = "org.sagebionetworks.synapse.user.profile.validation.message.visible.state";
	
	public static int PROFILE = 0x1;
	public static int ORC_ID = 0x2;
	public static int VERIFICATION_SUBMISSION = 0x4;
	public static int IS_CERTIFIED = 0x8;
	public static int IS_VERIFIED = 0x10;
	public static int IS_ACT_MEMBER = 0x20;
	
	private Profile place;
	private ProfileView view;
	private AuthenticationController authenticationController;
	private GlobalApplicationState globalApplicationState;
	private UserProfileModalWidget userProfileModalWidget;
	private GWTWrapper gwt;
	private OpenTeamInvitationsWidget openInvitesWidget;
	
	private SettingsPresenter settingsPresenter;
	private PortalGinInjector ginInjector;
	private int inviteCount;
	private int openRequestCount;
	private String currentUserId;
	private boolean isOwner;
	private int currentProjectOffset, currentChallengeOffset;
	private String teamNextPageToken;
	private boolean includeRequestCount;
	public final static int PROJECT_PAGE_SIZE=20;
	public final static int CHALLENGE_PAGE_SIZE=20;
	public ProfileArea currentArea;
	public ProjectFilterEnum filterType;
	public String filterTeamId;
	
	public SortDirection currentSortDirection;
	public ProjectListSortColumn currentSortColumn;
	public TeamListWidget myTeamsWidget;
	public LoadMoreWidgetContainer loadMoreTeamsWidgetContainer;
	public SynapseAlert profileSynAlert;
	public SynapseAlert projectSynAlert;
	public SynapseAlert teamSynAlert;
	public SynapseAlert challengeSynAlert;
	public UserBundle currentUserBundle;
	public LoadMoreWidgetContainer loadMoreProjectsWidgetContainer;
	public Callback getMoreProjectsCallback, getMoreTeamsCallback;
	public Callback refreshTeamsCallback;
	
	public PromptModalView promptForProjectNameDialog;
	public PromptModalView promptForTeamNameDialog;
	public SynapseJavascriptClient jsClient;
	public DownloadListWidget downloadListWidget;
	@Inject
	public ProfilePresenter(ProfileView view,
			AuthenticationController authenticationController,
			GlobalApplicationState globalApplicationState,
			GWTWrapper gwt,
			TeamListWidget myTeamsWidget,
			OpenTeamInvitationsWidget openInvitesWidget,
			PortalGinInjector ginInjector,
			SynapseJavascriptClient jsClient) {
		this.view = view;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.ginInjector = ginInjector;
		this.gwt = gwt;
		this.myTeamsWidget = myTeamsWidget;
		this.openInvitesWidget = openInvitesWidget;
		this.jsClient = jsClient;
		profileSynAlert = ginInjector.getSynapseAlertWidget();
		projectSynAlert = ginInjector.getSynapseAlertWidget();
		teamSynAlert = ginInjector.getSynapseAlertWidget();
		challengeSynAlert = ginInjector.getSynapseAlertWidget();
		view.setPresenter(this);
		view.addOpenInvitesWidget(openInvitesWidget);
		view.setProfileSynAlertWidget(profileSynAlert.asWidget());
		view.setProjectSynAlertWidget(projectSynAlert.asWidget());
		view.setTeamSynAlertWidget(teamSynAlert.asWidget());
		view.setChallengeSynAlertWidget(challengeSynAlert.asWidget());
		getMoreProjectsCallback = () -> {
			getMoreProjects();
		};
		
		getMoreTeamsCallback = () -> {
			getMoreTeams();
		};
		
		refreshTeamsCallback = () -> {
			refreshTeamsForFilter();
		};
	}
	
	public PromptModalView getPromptForTeamNameDialog() {
		if (promptForTeamNameDialog == null) {
			promptForTeamNameDialog = ginInjector.getPromptModal();
			promptForTeamNameDialog.setPresenter(new PromptModalView.Presenter() {
				@Override
				public void onPrimary() {
					createTeamAfterPrompt();
				}
			});
		}
		return promptForTeamNameDialog;
	}
	
	public PromptModalView getPromptForProjectNameDialog() {
		if (promptForProjectNameDialog == null) {
			promptForProjectNameDialog = ginInjector.getPromptModal();
			promptForProjectNameDialog.setPresenter(new PromptModalView.Presenter() {
				@Override
				public void onPrimary() {
					createProjectAfterPrompt();
				}
			});
		}
		return promptForProjectNameDialog;
	}

	
	public UserProfileModalWidget getUserProfileModalWidget() {
		if (userProfileModalWidget == null) {
			userProfileModalWidget = ginInjector.getUserProfileModalWidget();
			view.addUserProfileModalWidget(userProfileModalWidget);
		}
		return userProfileModalWidget;
	}

	public SettingsPresenter getSettingsPresenter() {
		if (settingsPresenter == null) {
			settingsPresenter = ginInjector.getSettingsPresenter();
		}
		return settingsPresenter;
	}
	
	public DownloadListWidget getDownloadListWidget() {
		if (downloadListWidget == null) {
			downloadListWidget = ginInjector.getDownloadListWidget();
		}
		return downloadListWidget;
	}
	
	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
		
	}
	
	@Override
	public void setPlace(Profile place) {
		this.place = place;
		this.view.clear();
		resetSynAlertWidgets();
		showView(place);
		view.setPresenter(this);
	}
	
	private void resetSynAlertWidgets() {
		profileSynAlert.clear();
		projectSynAlert.clear();	
		teamSynAlert.clear();	
	}
	
	@Override
    public String mayStop() {
        view.clear();
        return null;
    }
	
	public void editMyProfile() {
		if (checkIsLoggedIn())
			goTo(new Profile(authenticationController.getCurrentUserPrincipalId(), ProfileArea.SETTINGS));
	}
	
	public void viewMyProfile() {
		if (checkIsLoggedIn())
			goTo(new Profile(authenticationController.getCurrentUserPrincipalId()));
	}
	
	@Override
	public void goTo(Place place) {
		globalApplicationState.getPlaceChanger().goTo(place);
	}
	
	public void updateArea(ProfileArea area, boolean pushState) {
		currentArea = area;
		if (area != null && place != null && !area.equals(place.getArea())) {
			place.setArea(area, filterType, filterTeamId);
			if (pushState) {
				globalApplicationState.pushCurrentPlace(place);	
			} else {
				globalApplicationState.replaceCurrentPlace(place);
			}
		}
	}
	
	// Configuration
	public void updateProfileView(String userId) {
		currentSortColumn = ProjectListSortColumn.LAST_ACTIVITY;
		currentSortDirection = SortDirection.DESC;
		inviteCount = 0;
		openRequestCount = 0;
		isOwner = authenticationController.isLoggedIn()
				&& authenticationController.getCurrentUserPrincipalId().equals(
						userId);
		if (currentArea == null || (ProfileArea.SETTINGS.equals(currentArea) && !isOwner)) {
			currentArea = ProfileArea.PROFILE;
		}
		view.clear();
		view.showLoading();
		view.setProfileEditButtonVisible(isOwner);
		view.showTabs(isOwner);
		if (settingsPresenter != null) {
			settingsPresenter.clear();	
		}
		myTeamsWidget.clear();
		view.clearTeamNotificationCount();
		currentUserId = userId == null ? authenticationController.getCurrentUserPrincipalId() : userId;
		if (isOwner) {
			// make sure we have the user favorites before continuing
			initUserFavorites(new Callback() {
				@Override
				public void invoke() {
					getUserProfile();
					boolean pushState = false;
					showTab(currentArea, pushState);
				}
			});
		} else {
			getUserProfile();
			boolean pushState = false;
			showTab(currentArea, pushState);
		}
	}
	
	private void getUserProfile() {
		//ask for everything in the user bundle
		currentUserBundle = null;
		int mask = PROFILE | ORC_ID | VERIFICATION_SUBMISSION | IS_VERIFIED;
		Long currentUserIdLong = currentUserId != null ?  Long.parseLong(currentUserId)  : null;
		jsClient.getUserBundle(currentUserIdLong, mask, new AsyncCallback<UserBundle>() {
			@Override
			public void onSuccess(UserBundle bundle) {
				view.hideLoading();
				currentUserBundle = bundle;
				view.setProfile(bundle.getUserProfile(), isOwner, bundle.getORCID());
			}
			@Override
			public void onFailure(Throwable caught) {
				view.hideLoading();
				profileSynAlert.handleException(caught);
			}
		});
	}
	
	public void refreshProjects() {
		currentProjectOffset = 0;
		loadMoreProjectsWidgetContainer = ginInjector.getLoadMoreProjectsWidgetContainer();
		view.setProjectContainer(loadMoreProjectsWidgetContainer.asWidget());
		loadMoreProjectsWidgetContainer.setIsMore(false);
		loadMoreProjectsWidgetContainer.configure(getMoreProjectsCallback);
		getMoreProjects();
		scheduleRefreshTeamsIfOwner();
	}
	
	public void refreshChallenges() {
		currentChallengeOffset = 0;
		view.clearChallenges();
		getMoreChallenges();
	}
	
	/**
	 * Sets the project filter.  If filtered to a specific team, then the Team argument will be used.
	 * @param filterType
	 * @param team
	 */
	public void setProjectFilterAndRefresh(ProjectFilterEnum filterType, String filterTeamId) {
		if (filterType == null) {
			filterType = ProjectFilterEnum.ALL;
		}
		this.filterType = filterType;
		this.filterTeamId = filterTeamId;
		if (place != null)
			place.setArea(ProfileArea.PROJECTS, filterType, filterTeamId);
		refreshProjects();
	}

	public void getMoreProjects() {
		if (isOwner) {
			view.setLastActivityOnColumnVisible(true);
			view.showProjectFiltersUI();
			//this depends on the active filter
			switch (filterType) {
				case ALL:
					view.setAllProjectsFilterSelected();
					getMyProjects(ProjectListType.MY_PROJECTS, ProjectFilterEnum.ALL, currentProjectOffset);
					break;
				case CREATED_BY_ME:
					view.setMyProjectsFilterSelected();
					getMyProjects(ProjectListType.MY_CREATED_PROJECTS, ProjectFilterEnum.CREATED_BY_ME, currentProjectOffset);
					break;
				case SHARED_DIRECTLY_WITH_ME:
					view.setSharedDirectlyWithMeFilterSelected();
					getMyProjects(ProjectListType.MY_PARTICIPATED_PROJECTS, ProjectFilterEnum.SHARED_DIRECTLY_WITH_ME, currentProjectOffset);
					break;
				case ALL_MY_TEAM_PROJECTS:
					view.setTeamsFilterSelected();
					getMyProjects(ProjectListType.MY_TEAM_PROJECTS, ProjectFilterEnum.ALL_MY_TEAM_PROJECTS, currentProjectOffset);
					break;
				case FAVORITES:
					view.setFavoritesFilterSelected();
					view.setLastActivityOnColumnVisible(false);
					getFavorites();
					break;
				case TEAM:
					view.setTeamsFilterSelected();
					getTeamProjects(currentProjectOffset);
					break;
				default:
					break;
			}
		} else
			getUserProjects(currentProjectOffset);
	}
	
	@Override
	public void sort(ProjectListSortColumn column) {
		currentSortColumn = column;
		currentSortDirection = SortDirection.ASC.equals(currentSortDirection) ? SortDirection.DESC : SortDirection.ASC;
		view.setSortDirection(currentSortColumn, currentSortDirection);
		refreshProjects();
	}
	
	public void refreshTeamsForFilter() {
		updateMembershipInvitationCount();
		updateMembershipRequestCount();
		getTeamBundles(false);
	}
	
	@Override
	public void refreshTeams() {
		updateMembershipInvitationCount();
		updateMembershipRequestCount();
		refreshTeamInvites();
		getTeamBundles(isOwner);
	}
	
	public void refreshTeamInvites() {
		if (isOwner) {
			openInvitesWidget.configure(new Callback() {
				@Override
				public void invoke() {
					//refresh the teams after joining one
					refreshTeams();
				}
			}, null);
		}
	}
	
	public void updateMembershipRequestCount() {
		if (isOwner) {
			openRequestCount = 0;
			jsClient.getOpenMembershipRequestCount(new AsyncCallback<Long>() {
				@Override
				public void onFailure(Throwable caught) {
					teamSynAlert.handleException(caught);
				}
				
				@Override
				public void onSuccess(Long count) {
					setMembershipRequestsCount(count.intValue());
				}
			});
		}
	}
	
	public void updateMembershipInvitationCount() {
		if (isOwner) {
			inviteCount = 0;
			jsClient.getOpenMembershipInvitationCount(new AsyncCallback<Long>() {
				@Override
				public void onFailure(Throwable caught) {
					teamSynAlert.handleException(caught);
				}
				
				@Override
				public void onSuccess(Long count) {
					setTeamInvitationsCount(count.intValue());
				}
			});
		}
	}
	
	public void getTeamBundles(boolean includeRequestCount) {
		this.includeRequestCount = includeRequestCount;
		teamSynAlert.clear();
		myTeamsWidget.clear();
		teamNextPageToken = null;
		
		loadMoreTeamsWidgetContainer = ginInjector.getLoadMoreProjectsWidgetContainer();
		view.setTeamsContainer(loadMoreTeamsWidgetContainer.asWidget());
		loadMoreTeamsWidgetContainer.setIsMore(false);
		loadMoreTeamsWidgetContainer.configure(getMoreTeamsCallback);
		if (myTeamsWidget.asWidget() != null) {
			myTeamsWidget.asWidget().removeFromParent();	
		}
		loadMoreTeamsWidgetContainer.add(myTeamsWidget.asWidget());
		view.addMyTeamProjectsFilter();
		getMoreTeams();
	}
	
	public void getMoreTeams() {
		jsClient.getUserTeams(currentUserId, true, teamNextPageToken)
			.addCallback(
				new FutureCallback<PaginatedTeamIds>() {
					@Override
					public void onSuccess(PaginatedTeamIds paginatedTeamIds) {
						teamNextPageToken = paginatedTeamIds.getNextPageToken();
						List<String> teamIds = paginatedTeamIds.getTeamIds();
						boolean isTeams = teamIds.size() > 0;
						loadMoreTeamsWidgetContainer.setIsMore(teamNextPageToken != null);
						view.setTeamsFilterVisible(isTeams);
						if (isTeams) {
							addTeams(teamIds);
						} else {
							myTeamsWidget.showEmpty();
						}
					}
	
					@Override
					public void onFailure(Throwable caught) {
						view.setTeamsFilterVisible(false);
						loadMoreTeamsWidgetContainer.setIsMore(false);
						teamSynAlert.handleException(caught);
					}
				},
				directExecutor()
		);
	}
	
	public void addTeams(List<String> teamIds) {
		jsClient.listTeams(teamIds)
			.addCallback(
				new FutureCallback<List<Team>>() {
					@Override
					public void onSuccess(List<Team> teams) {
						ProfileArea currentArea = place.getArea();
						switch (currentArea) {
							case PROJECTS:
								for (Team team: teams) {
									view.addTeamsFilterTeam(team);
								}
								break;
							case TEAMS:
								for (Team team: teams) {
									myTeamsWidget.addTeam(team);
								}
								break;
							default:
								break;
						}
						if (includeRequestCount) {
							for (Team team: teams) {
								getTeamRequestCount(currentUserId, team);
							}
						}
					}
	
					@Override
					public void onFailure(Throwable caught) {
						view.setTeamsFilterVisible(false);
						loadMoreTeamsWidgetContainer.setIsMore(false);
						teamSynAlert.handleException(caught);
					}
				},
				directExecutor()
		);
	}
	
	public void getTeamRequestCount(String userId, final Team team) {
		jsClient.getOpenMembershipRequestCount(team.getId(), new AsyncCallback<Long>() {
			@Override
			public void onFailure(Throwable caught) {
			}
			@Override
			public void onSuccess(Long count) {
				myTeamsWidget.setNotificationValue(team.getId(), count);
			}
		});
	}
	
	public void getMoreChallenges() {
		challengeSynAlert.clear();
		view.showChallengesLoading(true);
		jsClient.getChallenges(currentUserId, CHALLENGE_PAGE_SIZE, currentChallengeOffset, new AsyncCallback<List<Challenge>>() {
			@Override
			public void onSuccess(List<Challenge> challengeList) {
				addChallengeResults(challengeList);
				challengePageAdded(challengeList.size());
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showChallengesLoading(false);
				challengeSynAlert.handleException(caught);
			}
			
		});
	}
	
	public void getMyProjects(ProjectListType projectListType, final ProjectFilterEnum filter, int offset) {
		projectSynAlert.clear();
		jsClient.getMyProjects(projectListType, PROJECT_PAGE_SIZE, offset, currentSortColumn, currentSortDirection, new AsyncCallback<List<ProjectHeader>>() {
			@Override
			public void onSuccess(List<ProjectHeader> results) {
				if (filterType == filter) {
					addProjectResults(results);
					projectPageAdded(results.size());
				}
			}
			@Override
			public void onFailure(Throwable caught) {
				projectSynAlert.handleException(caught);
			}
		});
	}
	
	public void getTeamProjects(int offset) {
		projectSynAlert.clear();
		jsClient.getProjectsForTeam(filterTeamId, PROJECT_PAGE_SIZE, offset, currentSortColumn, currentSortDirection, new AsyncCallback<List<ProjectHeader>>(){
			@Override
			public void onSuccess(List<ProjectHeader> results) {
				if (filterType == ProjectFilterEnum.TEAM) {
					addProjectResults(results);
					projectPageAdded(results.size());
				}
			}
			@Override
			public void onFailure(Throwable caught) {
				projectSynAlert.handleException(caught);
			}
		});
	}

	public void getUserProjects(int offset) {
		projectSynAlert.clear();
		jsClient.getUserProjects(currentUserId, PROJECT_PAGE_SIZE, offset, currentSortColumn, currentSortDirection, new AsyncCallback<List<ProjectHeader>>() {
			@Override
			public void onSuccess(List<ProjectHeader> results) {
				addProjectResults(results);
				projectPageAdded(results.size());
			}
			@Override
			public void onFailure(Throwable caught) {
				projectSynAlert.handleException(caught);
			}
		});
	}
	
	public void addProjectResults(List<ProjectHeader> projectHeaders) {
		for (int i = 0; i < projectHeaders.size(); i++) {
			ProjectBadge badge = ginInjector.getProjectBadgeWidget();
			badge.configure(projectHeaders.get(i));
			Widget widget = badge.asWidget();
			loadMoreProjectsWidgetContainer.add(widget);
		}
	}
	
	public void addChallengeResults(List<Challenge> challenges) {
		view.showChallengesLoading(false);
		for (Challenge challenge : challenges) {
			ChallengeBadge badge = ginInjector.getChallengeBadgeWidget();
			badge.configure(challenge);
			Widget widget = badge.asWidget();
			view.addChallengeWidget(widget);
		}
	}
	
	public void projectPageAdded(int projectsAdded) {
		currentProjectOffset += PROJECT_PAGE_SIZE;
		loadMoreProjectsWidgetContainer.setIsMore(projectsAdded >= PROJECT_PAGE_SIZE);
	}
	
	public void challengePageAdded(int challengesAdded) {
		currentChallengeOffset += CHALLENGE_PAGE_SIZE;
		view.setIsMoreChallengesVisible(challengesAdded >= CHALLENGE_PAGE_SIZE);
	}
	
	public void getFavorites() {
		projectSynAlert.clear();
		EntityBrowserUtils.loadFavorites(jsClient, globalApplicationState, new AsyncCallback<List<EntityHeader>>() {
			@Override
			public void onSuccess(List<EntityHeader> result) {
				if (filterType == ProjectFilterEnum.FAVORITES) {
					//convert to Project Headers
					if (result.size() == 0) {
						view.setFavoritesHelpPanelVisible(true);
					} else {
						List<ProjectHeader> headers = new ArrayList<ProjectHeader>(result.size());
						List<String> lastModifiedBy = new ArrayList<String>(result.size());
						for (EntityHeader header : result) {
							lastModifiedBy.add(header.getId());
							ProjectHeader projectHeader = new ProjectHeader();
							projectHeader.setId(header.getId());
							projectHeader.setName(header.getName());
							headers.add(projectHeader);
						}
						addProjectResults(headers);
						loadMoreProjectsWidgetContainer.setIsMore(false);
					}
				}
			}
			@Override
			public void onFailure(Throwable caught) {
				projectSynAlert.handleException(caught);
			}
		});
	}
	
	@Override
	public void createProject() {
		//prompt for project name
		getPromptForProjectNameDialog().configure("Create a New Project", "Project Name", "OK", "");
		getPromptForProjectNameDialog().show();
	}
	
	public void createProjectAfterPrompt() {
		final String name = getPromptForProjectNameDialog().getValue();
		//validate project name
		if (!DisplayUtils.isDefined(name)) {
			getPromptForProjectNameDialog().showError(DisplayConstants.PLEASE_ENTER_PROJECT_NAME);
			return;
		}
		Project project = new Project();
		project.setName(name);
		jsClient.createEntity(project)
			.addCallback(
					new FutureCallback<Entity>() {
						@Override
						public void onSuccess(Entity entity) {
							getPromptForProjectNameDialog().hide();
							view.showInfo(DisplayConstants.LABEL_PROJECT_CREATED + name);
							globalApplicationState.getPlaceChanger().goTo(new Synapse(entity.getId()));	
						}
	
						@Override
						public void onFailure(Throwable caught) {
							if(caught instanceof ConflictException) {
								getPromptForProjectNameDialog().showError(DisplayConstants.WARNING_PROJECT_NAME_EXISTS);
							} else {
								getPromptForProjectNameDialog().showError(caught.getMessage());
							}
						}
					},
					directExecutor()
		);
	}

	@Override
	public void createTeam() {
		// prompt for team name
		getPromptForTeamNameDialog().configure("Create a New Team", "Team Name", "OK", "");
		getPromptForTeamNameDialog().show();
	}
	
	public void createTeamAfterPrompt() {
		final String teamName = getPromptForTeamNameDialog().getValue();
		//validate team name
		if (!DisplayUtils.isDefined(teamName)) {
			getPromptForTeamNameDialog().showError(DisplayConstants.PLEASE_ENTER_TEAM_NAME);
			return;
		}
		
		Team newTeam = new Team();
		newTeam.setName(teamName);
		newTeam.setCanPublicJoin(false);
		jsClient.createTeam(newTeam, new AsyncCallback<Team>() {
			@Override
			public void onSuccess(Team team) {
				getPromptForTeamNameDialog().hide();
				view.showInfo(DisplayConstants.LABEL_TEAM_CREATED + teamName);
				globalApplicationState.getPlaceChanger().goTo(new org.sagebionetworks.web.client.place.Team(team.getId()));						
			}
			
			@Override
			public void onFailure(Throwable caught) {
				if(caught instanceof ConflictException) {
					getPromptForTeamNameDialog().showError(DisplayConstants.WARNING_TEAM_NAME_EXISTS);
				} else {
					getPromptForTeamNameDialog().showError(caught.getMessage());
				}
			}
		});		
	}
	
	private boolean checkIsLoggedIn() {
		if (!authenticationController.isLoggedIn()) {
			view.showErrorMessage(DisplayConstants.ERROR_LOGIN_REQUIRED);
			globalApplicationState.getPlaceChanger().goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
			return false;
		}
		return true;
	}
	
	private void profileUpdated() {
		view.showInfo("Your profile has been successfully updated.");
		updateProfileView(currentUserId);
		view.refreshHeader();
	}
	
	private void showView(Profile place) {
		view.clear();
		profileSynAlert.clear();
		projectSynAlert.clear();
		teamSynAlert.clear();
		String token = place.toToken();
		currentArea = place.getArea();
		filterType = place.getProjectFilter();
		filterTeamId = place.getTeamId();
		if (loadMoreProjectsWidgetContainer != null) {
			loadMoreProjectsWidgetContainer.clear();
		}
		if (loadMoreTeamsWidgetContainer != null) {
			loadMoreTeamsWidgetContainer.clear();
		}
		
		if (token.equals("oauth_bound")) {
			view.showInfo(DisplayConstants.SUCCESSFULLY_LINKED_OAUTH2_ACCOUNT);
			globalApplicationState.getPlaceChanger().goTo(new Profile(authenticationController.getCurrentUserPrincipalId(), ProfileArea.SETTINGS));
			return;
		}
		if (token.equals(Profile.VIEW_PROFILE_TOKEN) || token.startsWith(Profile.VIEW_PROFILE_TOKEN + "/") || token.isEmpty()) {
			Place gotoPlace = null;
			if (authenticationController.isLoggedIn()) {
				//replace url with current user id
				token = authenticationController.getCurrentUserPrincipalId() + token.substring(1);
				gotoPlace = new Profile(token);
			} else {
				//does not make sense, go home
				gotoPlace = new Home(ClientProperties.DEFAULT_PLACE_TOKEN);
			}
			globalApplicationState.getPlaceChanger().goTo(gotoPlace);
			return;
		}
		if (authenticationController.isLoggedIn() && authenticationController.getCurrentUserPrincipalId().equals(place.getUserId())) {
			//View my profile
			updateProfileView(place.getUserId());
		}
		else {
			if (Profile.EDIT_PROFILE_TOKEN.equals(token)) {
				editMyProfile();
			} else {
				//if this is a number, then treat it as a a user id
				try{
					Long.parseLong(place.getUserId());
					updateProfileView(place.getUserId());
				} catch (NumberFormatException nfe) {
					getUserIdFromUsername(token);
				}
			}
		}
	}
	
	public void getUserIdFromUsername(String userName) {
		PrincipalAliasRequest request = new PrincipalAliasRequest();
		request.setAlias(userName);
		request.setType(AliasType.USER_NAME);
		jsClient.getPrincipalAlias(request, new AsyncCallback<PrincipalAliasResponse>() {
			@Override
			public void onSuccess(PrincipalAliasResponse principalAlias) {
				String userId = principalAlias.getPrincipalId().toString();
				place.setUserId(userId);
				updateProfileView(userId);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				profileSynAlert.handleException(caught);
			}
		});
	}
	
	public void setTeamInvitationsCount(int count) {
		inviteCount = count;
		refreshTeamNotificationCount();
	}

	public void setMembershipRequestsCount(int count) {
		openRequestCount = count;		
		refreshTeamNotificationCount();
	}
	
	public void refreshTeamNotificationCount() {
		if (openRequestCount + inviteCount > 0) {
			view.setTeamNotificationCount(Integer.toString(openRequestCount + inviteCount));
		} else {
			view.clearTeamNotificationCount();
		}
	}
	
	/**
	 * Exposed for test purposes only
	 */
	public int getOpenRequestCount() {
		return openRequestCount;
	}
	
	public int getInviteCount() {
		return inviteCount;
	}
	
	public void setOpenRequestCount(int openRequestCount) {
		this.openRequestCount = openRequestCount;
	}
	
	public void setInviteCount(int inviteCount) {
		this.inviteCount = inviteCount;
	}
	
	/**
	 * Exposed for unit testing purposes only
	 * @return
	 */
	public int getCurrentOffset() {
		return currentProjectOffset;
	}

	/**
	 * Exposed for unit testing purposes only
	 * @return
	 */
	public void setCurrentOffset(int currentOffset) {
		this.currentProjectOffset = currentOffset;
	}
	
	/**
	 * Exposed for unit testing purposes only
	 * @return
	 */
	public int getCurrentChallengeOffset() {
		return currentChallengeOffset;
	}
	
	/**
	 * Exposed for unit testing purposes only
	 * @return
	 */
	public boolean isOwner() {
		return isOwner;
	}
	
	public void showTab(ProfileArea tab, boolean pushState) {
		updateArea(tab, pushState);
		refreshData(tab);
		view.setTabSelected(tab);
	}
	
	@Override
	public void tabClicked(final ProfileArea tab) {
		if (tab == null) {
			view.showErrorMessage("The selected tab is undefined.");
			return;
		}
		// if we are editing, then pop up a confirm
		if (globalApplicationState.isEditing()) {
			Callback yesCallback = new Callback() {
				@Override
				public void invoke() {
					boolean pushState = true;
					showTab(tab, pushState);
				}
			};
			view.showConfirmDialog("",
					DisplayConstants.NAVIGATE_AWAY_CONFIRMATION_MESSAGE,
					yesCallback);
		} else {
			boolean pushState = true;
			showTab(tab, pushState);
		}
	}
	
	private void refreshData(ProfileArea tab) {
		switch (tab) {
			case PROFILE:
				//update teams (for notification count)
				scheduleRefreshTeamsIfOwner();
				break;
			case PROJECTS:
				setProjectFilterAndRefresh(filterType, filterTeamId);
				break;
			case TEAMS:
				refreshTeams();
				break;
			case SETTINGS:
				getSettingsPresenter().configure();
				view.setSettingsWidget(getSettingsPresenter().asWidget());
				break;
			case CHALLENGES:
				refreshChallenges();
				break;
			case DOWNLOADS:
				getDownloadListWidget().refresh();
				view.setDownloadListWidget(getDownloadListWidget().asWidget());
				break;
			default:
				break;
		}
	}
	
	private void scheduleRefreshTeamsIfOwner() {
		if (isOwner) {
			// refresh owner teams to update the team notification count, and team filter
			gwt.scheduleExecution(refreshTeamsCallback, DELAY_GET_MY_TEAMS);
		}
	}
	
	/**
	 * Exposed for unit testing purposes only
	 * @return
	 */
	public void setIsOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}
	
	public void initUserFavorites(final Callback callback) {
		jsClient.getFavorites(new AsyncCallback<List<EntityHeader>>() {
			@Override
			public void onSuccess(List<EntityHeader> favorites) {
				globalApplicationState.setFavorites(favorites);
				callback.invoke();
			}
			@Override
			public void onFailure(Throwable caught) {
				callback.invoke();
			}
		});
	}
	
	@Override
	public void applyFilterClicked(ProjectFilterEnum filterType, Team team) {
		String filterTeamId = null;
		if (team != null) {
			filterTeamId = team.getId();
		}
		setProjectFilterAndRefresh(filterType, filterTeamId);
		globalApplicationState.pushCurrentPlace(place);
	}
	
	/**
	 * For testing purposes only
	 * @param currentUserId
	 */
	public void setCurrentUserId(String currentUserId) {
		this.currentUserId = currentUserId;
	}
	
	/**
	 * For testing purposes only
	 */
	public ProjectFilterEnum getFilterType() {
		return filterType;
	}

	@Override
	public void onEditProfile() {
		getUserProfileModalWidget().showEditProfile(this.currentUserId, new Callback() {
			@Override
			public void invoke() {
				profileUpdated();
			}
		});
		
	}

	
	// used for targeted unit test only
	public void setLoadMoreProjectsWidgetContainer(LoadMoreWidgetContainer loadMoreProjectsWidgetContainer) {
		this.loadMoreProjectsWidgetContainer = loadMoreProjectsWidgetContainer;
	}
}

