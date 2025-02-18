package org.sagebionetworks.web.unitclient.widget.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.cache.EntityId2BundleCache;
import org.sagebionetworks.web.client.cache.EntityId2BundleCacheImpl;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.events.ChangeSynapsePlaceEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.place.Synapse.EntityArea;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.entity.EntityMetadata;
import org.sagebionetworks.web.client.widget.entity.EntityPageTop;
import org.sagebionetworks.web.client.widget.entity.EntityPageTopView;
import org.sagebionetworks.web.client.widget.entity.WikiPageWidget;
import org.sagebionetworks.web.client.widget.entity.controller.EntityActionController;
import org.sagebionetworks.web.client.widget.entity.file.BasicTitleBar;
import org.sagebionetworks.web.client.widget.entity.menu.v2.ActionMenuWidget;
import org.sagebionetworks.web.client.widget.entity.tabs.ChallengeTab;
import org.sagebionetworks.web.client.widget.entity.tabs.DatasetsTab;
import org.sagebionetworks.web.client.widget.entity.tabs.DiscussionTab;
import org.sagebionetworks.web.client.widget.entity.tabs.DockerTab;
import org.sagebionetworks.web.client.widget.entity.tabs.FilesTab;
import org.sagebionetworks.web.client.widget.entity.tabs.Tab;
import org.sagebionetworks.web.client.widget.entity.tabs.TablesTab;
import org.sagebionetworks.web.client.widget.entity.tabs.Tabs;
import org.sagebionetworks.web.client.widget.entity.tabs.WikiTab;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.binder.EventBinder;

@RunWith(MockitoJUnitRunner.class)
public class EntityPageTopTest {

	@Mock
	EntityPageTopView mockView;
	@Mock
	EntityBundle mockProjectBundle;
	@Mock
	EntityHeader mockProjectHeader;
	@Mock
	Project mockProjectEntity;
	@Mock
	EntityBundle mockEntityBundle;
	@Mock
	FileEntity mockFileEntity;
	@Mock
	TableEntity mockTableEntity;
	@Mock
	Dataset mockDatasetEntity;
	@Mock
	DockerRepository mockDockerEntity;
	@Mock
	SynapseClientAsync mockSynapseClientAsync;
	@Mock
	Tabs mockTabs;
	@Mock
	EntityMetadata mockProjectMetadata;
	@Mock
	WikiTab mockWikiTab;
	@Mock
	Tab mockWikiInnerTab;
	@Mock
	FilesTab mockFilesTab;
	@Mock
	Tab mockFilesInnerTab;
	@Mock
	DatasetsTab mockDatasetsTab;
	@Mock
	TablesTab mockTablesTab;
	@Mock
	Tab mockTablesInnerTab;
	@Mock
	Tab mockDatasetsInnerTab;
	@Mock
	ChallengeTab mockChallengeTab;
	@Mock
	Tab mockChallengeInnerTab;
	@Mock
	DiscussionTab mockDiscussionTab;
	@Mock
	DockerTab mockDockerTab;
	@Mock
	Tab mockDiscussionInnerTab;
	@Mock
	Tab mockDockerInnerTab;
	@Mock
	UserEntityPermissions mockPermissions;
	@Mock
	EntityActionController mockProjectActionController;
	@Mock
	ActionMenuWidget mockProjectActionMenuWidget;
	@Mock
	AccessControlList mockACL;
	@Mock
	CookieProvider mockCookies;
	@Mock
	BasicTitleBar mockProjectTitleBar;
	@Mock
	SynapseJavascriptClient mockSynapseJavascriptClient;
	@Mock
	GlobalApplicationState mockGlobalApplicationState;
	@Mock
	PlaceChanger mockPlaceChanger;
	@Mock
	Link mockLinkEntity;
	@Mock
	Reference mockLinkReference;
	@Mock
	EventBus mockEventBus;
	@Captor
	ArgumentCaptor<WikiPageWidget.Callback> wikiCallbackCaptor;
	@Captor
	ArgumentCaptor<Place> placeCaptor;
	@Mock
	EventBinder mockEventBinder;
	@Mock
	EntityHeader mockProjectEntityHeader;

	EntityId2BundleCache entityId2BundleCache;
	EntityPageTop pageTop;
	String projectEntityId = "syn123";
	String projectName = "fooooo";
	String projectWikiId = "31415926666";
	String userId = "1234567";
	boolean canEdit = true;
	boolean canModerate = false;

	@Before
	public void setUp() {
		when(mockFilesTab.asTab()).thenReturn(mockFilesInnerTab);
		when(mockWikiTab.asTab()).thenReturn(mockWikiInnerTab);
		when(mockTablesTab.asTab()).thenReturn(mockTablesInnerTab);
		when(mockDatasetsTab.asTab()).thenReturn(mockDatasetsInnerTab);
		when(mockChallengeTab.asTab()).thenReturn(mockChallengeInnerTab);
		when(mockDiscussionTab.asTab()).thenReturn(mockDiscussionInnerTab);
		when(mockDockerTab.asTab()).thenReturn(mockDockerInnerTab);
		when(mockGlobalApplicationState.getPlaceChanger()).thenReturn(mockPlaceChanger);
		when(mockView.getEventBinder()).thenReturn(mockEventBinder);
		entityId2BundleCache = new EntityId2BundleCacheImpl();
		pageTop = new EntityPageTop(mockView, mockSynapseClientAsync, mockTabs, mockProjectTitleBar, mockProjectMetadata, mockWikiTab, mockFilesTab, mockDatasetsTab, mockTablesTab, mockChallengeTab, mockDiscussionTab, mockDockerTab, mockProjectActionController, mockProjectActionMenuWidget, mockCookies, mockSynapseJavascriptClient, mockGlobalApplicationState, entityId2BundleCache, mockEventBus);
		AsyncMockStubber.callSuccessWith(mockProjectBundle).when(mockSynapseJavascriptClient).getEntityBundleFromCache(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(mockEntityBundle).when(mockSynapseJavascriptClient).getEntityBundleForVersion(anyString(), anyLong(), any(EntityBundleRequest.class), any(AsyncCallback.class));

		when(mockProjectBundle.getEntity()).thenReturn(mockProjectEntity);
		when(mockProjectEntity.getId()).thenReturn(projectEntityId);
		when(mockProjectBundle.getRootWikiId()).thenReturn(projectWikiId);
		when(mockProjectHeader.getId()).thenReturn(projectEntityId);
		when(mockProjectHeader.getName()).thenReturn(projectName);
		when(mockProjectBundle.getPermissions()).thenReturn(mockPermissions);
		when(mockPermissions.getCanCertifiedUserEdit()).thenReturn(canEdit);
		when(mockPermissions.getCanModerate()).thenReturn(canModerate);
		when(mockProjectBundle.getAccessControlList()).thenReturn(mockACL);
		when(mockCookies.getCookie(DisplayUtils.SYNAPSE_TEST_WEBSITE_COOKIE_KEY)).thenReturn("true");
		EntityPath path = new EntityPath();
		path.setPath(Collections.singletonList(mockProjectEntityHeader));
		when(mockProjectEntityHeader.getType()).thenReturn(Project.class.getName());
		when(mockProjectBundle.getPath()).thenReturn(path);

		AsyncMockStubber.callSuccessWith(true).when(mockSynapseJavascriptClient).isWiki(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseJavascriptClient).isFileOrFolder(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseJavascriptClient).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseJavascriptClient).isDataset(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockSynapseJavascriptClient).isDocker(anyString(), any(AsyncCallback.class));

		when(mockWikiInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockFilesInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockTablesInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockDatasetsInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockChallengeInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockDiscussionInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockDockerInnerTab.isTabListItemVisible()).thenReturn(true);

		when(mockWikiInnerTab.isContentStale()).thenReturn(true);
		when(mockFilesInnerTab.isContentStale()).thenReturn(true);
		when(mockTablesInnerTab.isContentStale()).thenReturn(true);
		when(mockDatasetsInnerTab.isContentStale()).thenReturn(true);
		when(mockDiscussionInnerTab.isContentStale()).thenReturn(true);
		when(mockDockerInnerTab.isContentStale()).thenReturn(true);
		when(mockChallengeInnerTab.isContentStale()).thenReturn(true);
		when(mockTabs.getTabCount()).thenReturn(6);
		when(mockLinkEntity.getLinksTo()).thenReturn(mockLinkReference);
	}

	@Test
	public void testConstruction() {
		verify(mockView).setTabs(any(Widget.class));
		verify(mockView).setProjectMetadata(any(Widget.class));
		verify(mockView).setProjectActionMenu(any(Widget.class));
		verify(mockTabs).addTab(mockFilesInnerTab);
		verify(mockTabs).addTab(mockWikiInnerTab);
		verify(mockTabs).addTab(mockTablesInnerTab);
		verify(mockTabs).addTab(mockDatasetsInnerTab);
		verify(mockTabs).addTab(mockChallengeInnerTab);
		verify(mockTabs).addTab(mockDiscussionInnerTab);
		verify(mockTabs).addTab(mockDockerInnerTab);

		verify(mockEventBinder).bindEventHandlers(pageTop, mockEventBus);
	}

	@Test
	public void testSelectEntity() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;

		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);

		ArgumentCaptor<CallbackP> selectEntityCallback = ArgumentCaptor.forClass(CallbackP.class);

		verify(mockFilesTab).setEntitySelectedCallback(selectEntityCallback.capture());
		selectEntityCallback.getValue().invoke(null);
		verify(mockTabs).showTab(mockFilesInnerTab, true);

		verify(mockTablesTab).setEntitySelectedCallback(selectEntityCallback.capture());
		selectEntityCallback.getValue().invoke(null);
		verify(mockTabs).showTab(mockTablesInnerTab, true);

		verify(mockDatasetsTab).setEntitySelectedCallback(selectEntityCallback.capture());
		selectEntityCallback.getValue().invoke(null);
		verify(mockTabs).showTab(mockDatasetsInnerTab, true);

		verify(mockDockerTab).setEntitySelectedCallback(selectEntityCallback.capture());
		selectEntityCallback.getValue().invoke(null);
		verify(mockTabs).showTab(mockDockerInnerTab, true);
	}

	@Test
	public void testSelectTableListingClearsQueryToken() {
		EntityArea area = EntityArea.TABLES;
		String initialAreaToken = "query/eyJzcWwiOiJzZWxlY3Qg";
		Long versionNumber = null;

		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, initialAreaToken);

		assertEquals(initialAreaToken, pageTop.getTablesAreaToken());

		ArgumentCaptor<CallbackP> selectEntityCallback = ArgumentCaptor.forClass(CallbackP.class);

		verify(mockTablesTab).setEntitySelectedCallback(selectEntityCallback.capture());
		// select the project
		selectEntityCallback.getValue().invoke("syn123");
		verify(mockTabs).showTab(mockTablesInnerTab, true);
		assertNull(pageTop.getTablesAreaToken());
	}

	@Test
	public void testSelectDatasetListingClearsQueryToken() {
		EntityArea area = EntityArea.DATASETS;
		String initialAreaToken = "query/eyJzcWwiOiJzZWxlY3Qg";
		Long versionNumber = null;

		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, initialAreaToken);

		assertEquals(initialAreaToken, pageTop.getDatasetsAreaToken());

		ArgumentCaptor<CallbackP> selectEntityCallback = ArgumentCaptor.forClass(CallbackP.class);

		verify(mockDatasetsTab).setEntitySelectedCallback(selectEntityCallback.capture());
		// select the project
		selectEntityCallback.getValue().invoke("syn123");
		verify(mockTabs).showTab(mockDatasetsInnerTab, true);
		assertNull(pageTop.getDatasetsAreaToken());
	}


	@Test
	public void testConfigureWithProject() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);
		// Area was not defined for this project, should try to go to wiki tab by default.

		// Once to show the active tab, and once after configuration so that the place is pushed into the
		// history.
		verify(mockTabs, times(2)).showTab(mockWikiInnerTab, false);
		verify(mockProjectMetadata).configure(mockProjectBundle, null, mockProjectActionMenuWidget);
		verify(mockWikiInnerTab, atLeastOnce()).setContentStale(true);
		verify(mockWikiInnerTab).setContentStale(false);
		verify(mockView).scrollToTop();

		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(mockProjectBundle), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));

		// verify it never asks for the project bundle (SWC-4462)
		verify(mockSynapseJavascriptClient, never()).getEntityBundleFromCache(eq(projectEntityId), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient, never()).getEntityBundleForVersion(anyString(), anyLong(), any(EntityBundleRequest.class), any(AsyncCallback.class));

		// entity area for the project settings doesn't apply (so it's set to null).
		EntityArea projectSettingsEntityArea = null;
		verify(mockProjectActionController).configure(mockProjectActionMenuWidget, mockProjectBundle, true, projectWikiId, projectSettingsEntityArea);
		verify(mockProjectActionMenuWidget).setToolsButtonIcon(EntityPageTop.PROJECT_SETTINGS, null);

		verify(mockFilesTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab, never()).configure(mockProjectBundle, versionNumber);
		verify(mockTablesTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab, never()).configure(mockProjectBundle, versionNumber, areaToken);
		verify(mockDatasetsTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockDatasetsTab, never()).configure(mockProjectBundle, versionNumber, areaToken);
		verify(mockChallengeTab, never()).configure(projectEntityId, projectName, mockProjectBundle);
		verify(mockDiscussionTab, never()).configure(projectEntityId, projectName, mockProjectBundle, areaToken, canModerate);
		verify(mockDockerTab, never()).configure(mockProjectBundle, areaToken);

		clickAllTabs();

		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockProjectBundle, versionNumber);
		verify(mockTablesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab).configure(mockProjectBundle, versionNumber, areaToken);
		verify(mockDatasetsTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockDatasetsTab).configure(mockProjectBundle, versionNumber, areaToken);
		verify(mockChallengeTab).configure(projectEntityId, projectName, mockProjectBundle);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, mockProjectBundle, areaToken, canModerate);
		verify(mockChallengeTab).updateActionMenuCommands();
		verify(mockDiscussionTab).updateActionMenuCommands();
		verify(mockDockerTab).configure(mockProjectBundle, areaToken);
	}

	private void clickAllTabs() {
		// now go through and click on the tabs
		ArgumentCaptor<CallbackP> tabCaptor = ArgumentCaptor.forClass(CallbackP.class);

		// click on the wiki tab
		verify(mockWikiTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		verify(mockView).scrollToTop();

		// click on the files tab
		verify(mockFilesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		// click on the datasets tab
		verify(mockDatasetsTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		// click on the tables tab
		verify(mockTablesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		// click on the challenge tab
		verify(mockChallengeTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		// click on the discussion tab
		verify(mockDiscussionTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		// click on the docker tab
		verify(mockDockerTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
	}

	@Test
	public void testClear() {
		pageTop.clearState();
		verify(mockView).clear();
		verify(mockWikiTab).clear();
	}

	@Test
	public void testConfigureWithFile() {
		when(mockEntityBundle.getEntity()).thenReturn(mockFileEntity);
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = 5L;
		pageTop.configure(mockEntityBundle, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockFilesTab).resetView();
		verify(mockTablesTab).resetView();
		verify(mockDatasetsTab).resetView();

		verify(mockTabs).showTab(mockFilesInnerTab, false);
		verify(mockProjectMetadata).configure(mockProjectBundle, null, mockProjectActionMenuWidget);
		verify(mockProjectActionMenuWidget).setToolsButtonIcon(EntityPageTop.PROJECT_SETTINGS, null);

		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockEntityBundle, versionNumber);

		verify(mockWikiTab, never()).configure(eq(projectEntityId), eq(projectName), eq(mockProjectBundle), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockTablesTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab, never()).configure(any(EntityBundle.class), anyLong(), eq(areaToken));
		verify(mockDatasetsTab, never()).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockDatasetsTab, never()).configure(any(EntityBundle.class), anyLong(), eq(areaToken));
		verify(mockChallengeTab, never()).configure(projectEntityId, projectName, mockProjectBundle);
		verify(mockDiscussionTab, never()).configure(projectEntityId, projectName, mockProjectBundle, null, canModerate);
		verify(mockDockerTab, never()).configure(mockEntityBundle, null);

		// verify tab places have been set (allows for right-click of tab, before the tab has been
		// initialized)
		Long projectVersionNumber = null;
		Synapse expectedPlace = new Synapse(projectEntityId, projectVersionNumber, EntityArea.FILES, areaToken);

		verify(mockFilesInnerTab).setEntityNameAndPlace(projectName, expectedPlace);
		expectedPlace.setArea(EntityArea.WIKI);
		verify(mockWikiInnerTab).setEntityNameAndPlace(projectName, expectedPlace);
		expectedPlace.setArea(EntityArea.TABLES);
		verify(mockTablesInnerTab).setEntityNameAndPlace(projectName, expectedPlace);
		expectedPlace.setArea(EntityArea.DATASETS);
		verify(mockDatasetsInnerTab).setEntityNameAndPlace(projectName, expectedPlace);
		expectedPlace.setArea(EntityArea.CHALLENGE);
		verify(mockChallengeInnerTab).setEntityNameAndPlace(projectName, expectedPlace);
		expectedPlace.setArea(EntityArea.DISCUSSION);
		verify(mockDiscussionInnerTab).setEntityNameAndPlace(projectName, expectedPlace);
		expectedPlace.setArea(EntityArea.DOCKER);
		verify(mockDockerInnerTab).setEntityNameAndPlace(projectName, expectedPlace);

		clickAllTabsFile();
	}


	private void clickAllTabsFile() {
		// now go through and click on the tabs, verify project metadata visibility
		ArgumentCaptor<CallbackP> tabCaptor = ArgumentCaptor.forClass(CallbackP.class);

		// click on the wiki tab
		reset(mockProjectMetadata);
		verify(mockWikiTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		
		// click on the files tab
		verify(mockFilesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		verify(mockFilesInnerTab, times(2)).setContentStale(true);

		// click on the tables tab
		verify(mockTablesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		verify(mockTablesInnerTab).setContentStale(true);
		tabCaptor.getValue().invoke(null);
		verify(mockTablesInnerTab, times(2)).setContentStale(true);

		// click on the datasets tab
		verify(mockDatasetsTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		verify(mockDatasetsInnerTab).setContentStale(true);
		tabCaptor.getValue().invoke(null);
		verify(mockDatasetsInnerTab, times(2)).setContentStale(true);

		// click on the challenge tab
		verify(mockChallengeTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		
		// click on the discussion tab
		verify(mockDiscussionTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		verify(mockDiscussionInnerTab).setContentStale(true);
		tabCaptor.getValue().invoke(null);
		verify(mockDiscussionInnerTab, times(2)).setContentStale(true);

		// click on the docker tab
		verify(mockDockerTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		verify(mockDockerInnerTab).setContentStale(true);
		tabCaptor.getValue().invoke(null);
		verify(mockDockerInnerTab, times(2)).setContentStale(true);
	}

	@Test
	public void testConfigureWithFileAndFailureToLoadProject() {
		when(mockEntityBundle.getEntity()).thenReturn(mockFileEntity);
		Exception projectLoadError = new Exception("failed to load project");
		AsyncMockStubber.callFailureWith(projectLoadError).when(mockSynapseJavascriptClient).getEntityBundleFromCache(anyString(), any(AsyncCallback.class));
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = 5L;
		pageTop.configure(mockEntityBundle, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockFilesInnerTab, false);

		verify(mockProjectMetadata, Mockito.never()).configure(mockProjectBundle, null, null);
		EntityBundle expectedProjectEntityBundle = null;
		verify(mockFilesTab).setProject(projectEntityId, expectedProjectEntityBundle, projectLoadError);
		verify(mockFilesTab).configure(mockEntityBundle, versionNumber);

		clickAllTabs();
		// project bundle is null, unable to get wiki id or canEdit.
		String wikiId = null;
		canEdit = false;
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(null), eq(wikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockTablesTab).setProject(projectEntityId, null, projectLoadError);
		verify(mockTablesTab).configure(any(EntityBundle.class), eq(null), eq(areaToken));
		verify(mockDatasetsTab).setProject(projectEntityId, null, projectLoadError);
		verify(mockDatasetsTab).configure(any(EntityBundle.class), eq(null), eq(areaToken));
		verify(mockChallengeTab).configure(projectEntityId, projectName, null);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, null, null, canModerate);
		verify(mockDockerTab).configure(null, null);
	}

	@Test
	public void testConfigureWithTable() {
		when(mockEntityBundle.getEntity()).thenReturn(mockTableEntity);
		Synapse.EntityArea area = null;
		String areaToken = "a table query area token";
		Long versionNumber = 23L;
		pageTop.configure(mockEntityBundle, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockTablesInnerTab, false);

		verify(mockProjectMetadata).configure(mockProjectBundle, null, mockProjectActionMenuWidget);

		verify(mockTablesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockTablesTab).configure(mockEntityBundle, versionNumber, areaToken);

		clickAllTabsTable();
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(mockProjectBundle), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockProjectBundle, null);
		verify(mockDatasetsTab).configure(mockProjectBundle, null, null);
		verify(mockChallengeTab).configure(projectEntityId, projectName, mockProjectBundle);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, mockProjectBundle, null, canModerate);
		verify(mockDockerTab).configure(mockProjectBundle, null);
	}


	@Test
	public void testConfigureWithDataset() {
		when(mockEntityBundle.getEntity()).thenReturn(mockDatasetEntity);
		Synapse.EntityArea area = null;
		String areaToken = "a table query area token";
		Long versionNumber = 23L;
		pageTop.configure(mockEntityBundle, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockDatasetsInnerTab, false);

		verify(mockProjectMetadata).configure(mockProjectBundle, null, mockProjectActionMenuWidget);

		verify(mockDatasetsTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockDatasetsTab).configure(mockEntityBundle, versionNumber, areaToken);

		clickAllTabsTable();
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(mockProjectBundle), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockProjectBundle, null);
		verify(mockTablesTab).configure(mockProjectBundle, null, null);
		verify(mockChallengeTab).configure(projectEntityId, projectName, mockProjectBundle);
		verify(mockDiscussionTab).configure(projectEntityId, projectName, mockProjectBundle, null, canModerate);
		verify(mockDockerTab).configure(mockProjectBundle, null);
	}


	private void clickAllTabsTable() {
		// now go through and click on the tabs, verify project metadata visibility
		ArgumentCaptor<CallbackP> tabCaptor = ArgumentCaptor.forClass(CallbackP.class);

		// click on the wiki tab
		reset(mockProjectMetadata);
		verify(mockWikiTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
		
		// click on the files tab
		reset(mockProjectMetadata);
		verify(mockFilesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);

		// click on the tables tab
		reset(mockProjectMetadata);
		verify(mockTablesTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);

		// click on the datasets tab
		reset(mockProjectMetadata);
		verify(mockDatasetsTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);

		// click on the challenge tab
		reset(mockProjectMetadata);
		verify(mockChallengeTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);

		// click on the discussion tab
		reset(mockProjectMetadata);
		verify(mockDiscussionTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);

		// click on the docker tab
		reset(mockProjectMetadata);
		verify(mockDockerTab).setTabClickedCallback(tabCaptor.capture());
		tabCaptor.getValue().invoke(null);
	}

	@Test
	public void testConfigureWithDocker() {
		when(mockEntityBundle.getEntity()).thenReturn(mockDockerEntity);
		Synapse.EntityArea area = null;
		String areaToken = "docker area token";
		Long versionNumber = null;
		pageTop.configure(mockEntityBundle, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockTabs).showTab(mockDockerInnerTab, false);

		verify(mockProjectMetadata).configure(mockProjectBundle, null, mockProjectActionMenuWidget);

		verify(mockDockerTab).configure(mockEntityBundle, areaToken);
	}

	@Test
	public void testFireEntityUpdatedEvent() {
		pageTop.fireEntityUpdatedEvent();
		verify(mockEventBus).fireEvent(any(EntityUpdatedEvent.class));
	}

	@Test
	public void testConfigureWithFileGoToChallengeAdminTab() {
		when(mockEntityBundle.getEntity()).thenReturn(mockFileEntity);
		Synapse.EntityArea area = Synapse.EntityArea.CHALLENGE;
		String areaToken = null;
		Long versionNumber = null;
		pageTop.configure(mockEntityBundle, versionNumber, mockProjectHeader, area, areaToken);

		verify(mockProjectMetadata).configure(mockProjectBundle, null, mockProjectActionMenuWidget);
		// ignore specified area, target entity is a File so configure and show the Files tab
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockEntityBundle, versionNumber);
	}

	@Test
	public void testGetWikiPageId() {
		String areaToken = "123";
		String rootWikiId = "456";
		// should use wiki area token wiki id if available
		assertEquals(areaToken, pageTop.getWikiPageId(areaToken, rootWikiId));
		// and the root wiki id if area token is not defined
		assertEquals(rootWikiId, pageTop.getWikiPageId("", rootWikiId));
		assertEquals(rootWikiId, pageTop.getWikiPageId(null, rootWikiId));
	}

	@Test
	public void testConfigureProjectInvalidWikiId() {
		Synapse.EntityArea area = null;
		String invalidWikiId = "1234";
		Long versionNumber = null;
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, invalidWikiId);

		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(mockProjectBundle), eq(invalidWikiId), eq(canEdit), wikiCallbackCaptor.capture());

		when(mockWikiInnerTab.isContentStale()).thenReturn(true);
		// simulate not found
		wikiCallbackCaptor.getValue().noWikiFound();
		// since the project has a root wiki id, it should try to load that instead.
		verify(mockView).showInfo(anyString());
		verify(mockWikiTab).configure(eq(projectEntityId), eq(projectName), eq(mockProjectBundle), eq(projectWikiId), eq(canEdit), any(WikiPageWidget.Callback.class));
	}

	@Test
	public void testCurrentTabHidden() {
		when(mockWikiInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockFilesInnerTab.isTabListItemVisible()).thenReturn(true);
		when(mockTablesInnerTab.isTabListItemVisible()).thenReturn(false);
		when(mockDatasetsInnerTab.isTabListItemVisible()).thenReturn(false);
		when(mockDockerInnerTab.isTabListItemVisible()).thenReturn(false);
		
		Synapse.EntityArea area = EntityArea.WIKI;
		String areaToken = "1234";
		Long versionNumber = null;
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);

		// initially the wiki tab is configured since it's visible.
		verify(mockWikiTab).configure(anyString(), anyString(), any(EntityBundle.class), anyString(), anyBoolean(), any(WikiPageWidget.Callback.class));
		verify(mockFilesTab, never()).configure(mockProjectBundle, versionNumber);
		
		// but if it is not visible, then checking for tab visibility should cause the Files tab to be configured.
		when(mockWikiInnerTab.isTabListItemVisible()).thenReturn(false);
		
		pageTop.checkForTabVisibility();
		
		// we discover that the wiki tab is not visible, so the files tab is configured and shown.
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockProjectBundle, versionNumber);
	}

	@Test
	public void testConfigureProjectNoWiki() {
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isWiki(anyString(), any(AsyncCallback.class));
		when(mockWikiInnerTab.isTabListItemVisible()).thenReturn(false);
		// we are asking for an invalid wiki id for a project that contains no wiki.
		Synapse.EntityArea area = null;
		String invalidWikiId = "1234";
		Long versionNumber = null;
		when(mockProjectBundle.getRootWikiId()).thenReturn(null);
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, invalidWikiId);
		verify(mockWikiTab, never()).configure(anyString(), anyString(), any(EntityBundle.class), anyString(), anyBoolean(), any(WikiPageWidget.Callback.class));

		when(mockWikiInnerTab.isContentStale()).thenReturn(true);
		// since the project does not have a root wiki id, it should go to the files tab
		verify(mockFilesTab).setProject(projectEntityId, mockProjectBundle, null);
		verify(mockFilesTab).configure(mockProjectBundle, versionNumber);
	}

	@Test
	public void testContentMultipleTabsShown() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);
		verify(mockWikiInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockFilesInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockTablesInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockDatasetsInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockDiscussionInnerTab, atLeastOnce()).setTabListItemVisible(true);
		verify(mockDockerInnerTab, atLeastOnce()).setTabListItemVisible(true);
	}

	@Test
	public void testContentMixtureOfTabsShownCanEdit() {
		when(mockPermissions.getCanEdit()).thenReturn(true);
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;

		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isDataset(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isDocker(anyString(), any(AsyncCallback.class));

		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);

		verify(mockSynapseJavascriptClient, never()).isWiki(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient, never()).isFileOrFolder(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient, never()).isTable(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient, never()).isDataset(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient, never()).isDocker(anyString(), any(AsyncCallback.class));

		verify(mockWikiInnerTab).setTabListItemVisible(true);
		verify(mockFilesInnerTab).setTabListItemVisible(true);
		verify(mockTablesInnerTab).setTabListItemVisible(true);
		verify(mockDatasetsInnerTab).setTabListItemVisible(true);
		verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockChallengeInnerTab, never()).setTabListItemVisible(true);
		verify(mockDiscussionInnerTab).setTabListItemVisible(true);
		verify(mockDockerInnerTab).setTabListItemVisible(true);
	}

	@Test
	public void testContentMixtureOfTabsShownCannotEdit() {
		when(mockPermissions.getCanEdit()).thenReturn(false);
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;

		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isDataset(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isDocker(anyString(), any(AsyncCallback.class));

		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);

		verify(mockSynapseJavascriptClient).isWiki(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient).isFileOrFolder(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient).isTable(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient).isDataset(anyString(), any(AsyncCallback.class));
		verify(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		verify(mockSynapseJavascriptClient).isDocker(anyString(), any(AsyncCallback.class));

		InOrder order = Mockito.inOrder(mockWikiInnerTab);
		order.verify(mockWikiInnerTab).setTabListItemVisible(false);
		order.verify(mockWikiInnerTab).setTabListItemVisible(true);

		order = Mockito.inOrder(mockFilesInnerTab);
		order.verify(mockFilesInnerTab).setTabListItemVisible(false);
		order.verify(mockFilesInnerTab).setTabListItemVisible(true);

		order = Mockito.inOrder(mockDatasetsInnerTab);
		order.verify(mockDatasetsInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockDatasetsInnerTab, never()).setTabListItemVisible(true);

		order = Mockito.inOrder(mockTablesInnerTab);
		order.verify(mockTablesInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockTablesInnerTab, never()).setTabListItemVisible(true);

		order = Mockito.inOrder(mockChallengeInnerTab);
		order.verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockChallengeInnerTab, never()).setTabListItemVisible(true);

		order = Mockito.inOrder(mockDiscussionInnerTab);
		order.verify(mockDiscussionInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockDiscussionInnerTab).setTabListItemVisible(true);

		order = Mockito.inOrder(mockDockerInnerTab);
		order.verify(mockDockerInnerTab, atLeastOnce()).setTabListItemVisible(false);
		order.verify(mockDockerInnerTab, never()).setTabListItemVisible(true);
	}

	@Test
	public void testContentOneTabShown() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isFileOrFolder(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isTable(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isDataset(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseClientAsync).isChallenge(anyString(), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockSynapseJavascriptClient).isDocker(anyString(), any(AsyncCallback.class));

		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);
		// should hide all tabs when only one will be shown
		verify(mockWikiInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockFilesInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockTablesInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockDatasetsInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockChallengeInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockDiscussionInnerTab, atLeastOnce()).setTabListItemVisible(false);
		verify(mockDockerInnerTab, atLeastOnce()).setTabListItemVisible(false);
	}

	@Test
	public void testUpdateEntityBundleToLink() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);

		String targetEntityId = "syn2022";
		Long targetVersion = 4L;
		when(mockLinkReference.getTargetId()).thenReturn(targetEntityId);
		when(mockLinkReference.getTargetVersionNumber()).thenReturn(targetVersion);
		when(mockEntityBundle.getEntity()).thenReturn(mockLinkEntity);

		pageTop.updateEntityBundle(mockEntityBundle, null);

		verify(mockPlaceChanger).goTo(placeCaptor.capture());
		Synapse synPlace = (Synapse) placeCaptor.getValue();
		assertEquals(targetEntityId, synPlace.getEntityId());
		assertEquals(targetVersion, synPlace.getVersionNumber());
	}

	@Test
	public void testOnChangeSynapsePlaceDifferentEntityId() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		when(mockEntityBundle.getEntity()).thenReturn(mockProjectEntity);
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);
		Synapse newPlace = new Synapse("syn99");

		pageTop.onChangeSynapsePlace(new ChangeSynapsePlaceEvent(newPlace));

		verify(mockPlaceChanger).goTo(newPlace);
	}

	@Test
	public void testOnChangeSynapsePlaceSameEntityId() {
		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		when(mockEntityBundle.getEntity()).thenReturn(mockProjectEntity);
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);
		Synapse newPlace = new Synapse(projectEntityId, null, EntityArea.DISCUSSION, null);

		pageTop.onChangeSynapsePlace(new ChangeSynapsePlaceEvent(newPlace));

		verify(mockPlaceChanger, never()).goTo(newPlace);
		// configured the discussion tab
		verify(mockDiscussionTab).configure(projectEntityId, projectName, mockProjectBundle, areaToken, canModerate);
	}

	@Test
	public void testOnChangeSynapsePlaceDifferentEntityIdSameProject() {
		String entityId1 = "syn1";
		String entityId2 = "syn2";
		when(mockProjectEntity.getId()).thenReturn(entityId1);
		entityId2BundleCache.put(entityId1, mockProjectBundle);
		entityId2BundleCache.put(entityId2, mockProjectBundle);

		Synapse.EntityArea area = null;
		String areaToken = null;
		Long versionNumber = null;
		when(mockEntityBundle.getEntity()).thenReturn(mockProjectEntity);
		pageTop.configure(mockProjectBundle, versionNumber, mockProjectHeader, area, areaToken);
		Synapse newPlace = new Synapse(entityId2);

		pageTop.onChangeSynapsePlace(new ChangeSynapsePlaceEvent(newPlace));

		// verify we did not change place, but instead reconfigured for target entity under the same project
		verify(mockPlaceChanger, never()).goTo(newPlace);
	}
}
