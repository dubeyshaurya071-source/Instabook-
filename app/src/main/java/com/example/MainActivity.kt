package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.UiState
import com.example.data.model.VideoPost
import com.example.data.repository.InstabookRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.SharePostBottomSheet
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.VideoDetailScreen
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookTheme
import com.example.ui.viewmodel.InstabookViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_FEED = "feed"
const val ROUTE_EXPLORE = "explore"
const val ROUTE_CREATE = "create"
const val ROUTE_ALERTS = "alerts"
const val ROUTE_PROFILE = "profile"
const val ROUTE_CREATOR = "creator/{userId}"
const val ROUTE_VIDEO_DETAIL = "video/{postId}"

private data class NavTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val InstabookTabs = listOf(
    NavTabItem(ROUTE_FEED, R.string.nav_feed, Icons.Filled.Home, Icons.Outlined.Home, "nav_feed"),
    NavTabItem(ROUTE_EXPLORE, R.string.nav_explore, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    NavTabItem(ROUTE_CREATE, R.string.nav_create, Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "nav_create"),
    NavTabItem(ROUTE_ALERTS, R.string.nav_alerts, Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_alerts"),
    NavTabItem(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InstabookTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            InstabookMainApp(currentUserId = uid)
        }
    }
}

@Composable
fun InstabookMainApp(currentUserId: String) {
    val viewModel: InstabookViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                InstabookViewModel(
                    repository = InstabookRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val creatorsState by viewModel.publicCreatorsState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeCommentsPost by remember { mutableStateOf<VideoPost?>(null) }
    var activeSharePost by remember { mutableStateOf<VideoPost?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openCreatorProfile(targetUserId: String) {
        if (targetUserId == currentUserId) {
            navigateTopTab(ROUTE_PROFILE)
        } else {
            navController.navigate("creator/$targetUserId")
        }
    }

    fun openVideoDetail(postId: String) {
        navController.navigate("video/$postId")
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.White,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail && currentRoute in InstabookTabs.map { it.route }) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 4.dp
                    ) {
                        InstabookTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = unreadCount
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = InstabookDeepBlue,
                                    selectedTextColor = InstabookDeepBlue,
                                    indicatorColor = InstabookIceBlue,
                                    unselectedIconColor = InstabookSlate,
                                    unselectedTextColor = InstabookSlate
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = Color.White,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        InstabookTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = unreadCount
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = InstabookDeepBlue,
                                    selectedTextColor = InstabookDeepBlue,
                                    indicatorColor = InstabookIceBlue,
                                    unselectedIconColor = InstabookSlate,
                                    unselectedTextColor = InstabookSlate
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_FEED,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_FEED) {
                        FeedScreen(
                            viewModel = viewModel,
                            onOpenNotifications = { navigateTopTab(ROUTE_ALERTS) },
                            onOpenCreate = { navigateTopTab(ROUTE_CREATE) },
                            onOpenCreatorProfile = ::openCreatorProfile,
                            onOpenComments = { activeCommentsPost = it },
                            onOpenShare = { activeSharePost = it }
                        )
                    }

                    composable(ROUTE_EXPLORE) {
                        ExploreScreen(
                            viewModel = viewModel,
                            onOpenVideoDetail = ::openVideoDetail,
                            onOpenCreatorProfile = ::openCreatorProfile,
                            onOpenCreate = { navigateTopTab(ROUTE_CREATE) }
                        )
                    }

                    composable(ROUTE_CREATE) {
                        CreatePostScreen(
                            viewModel = viewModel,
                            onPostPublished = { navigateTopTab(ROUTE_FEED) }
                        )
                    }

                    composable(ROUTE_ALERTS) {
                        NotificationsScreen(
                            viewModel = viewModel,
                            onOpenPost = ::openVideoDetail,
                            onOpenCreatorProfile = ::openCreatorProfile,
                            onOpenExplore = { navigateTopTab(ROUTE_EXPLORE) }
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            targetUserId = currentUserId,
                            viewModel = viewModel,
                            showBackButton = false,
                            onBack = {},
                            onOpenVideoDetail = ::openVideoDetail,
                            onOpenCreatorProfile = ::openCreatorProfile,
                            onOpenCreate = { navigateTopTab(ROUTE_CREATE) }
                        )
                    }

                    composable(
                        route = ROUTE_CREATOR,
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { entry ->
                        val targetUid = entry.arguments?.getString("userId") ?: currentUserId
                        ProfileScreen(
                            targetUserId = targetUid,
                            viewModel = viewModel,
                            showBackButton = true,
                            onBack = { navController.popBackStack() },
                            onOpenVideoDetail = ::openVideoDetail,
                            onOpenCreatorProfile = ::openCreatorProfile,
                            onOpenCreate = { navigateTopTab(ROUTE_CREATE) }
                        )
                    }

                    composable(
                        route = ROUTE_VIDEO_DETAIL,
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { entry ->
                        val postId = entry.arguments?.getString("postId") ?: ""
                        VideoDetailScreen(
                            postId = postId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenCreatorProfile = ::openCreatorProfile,
                            onOpenComments = { activeCommentsPost = it },
                            onOpenShare = { activeSharePost = it }
                        )
                    }
                }
            }
        }
    }

    activeCommentsPost?.let { post ->
        CommentsBottomSheet(
            post = post,
            viewModel = viewModel,
            onAuthorClick = ::openCreatorProfile,
            onDismiss = { activeCommentsPost = null }
        )
    }

    activeSharePost?.let { post ->
        val creators = (creatorsState as? UiState.Success)?.data.orEmpty()
        SharePostBottomSheet(
            post = post,
            creators = creators,
            currentUserId = currentUserId,
            viewModel = viewModel,
            onDismiss = { activeSharePost = null }
        )
    }
}

@Composable
private fun TabIconWithBadge(
    tab: NavTabItem,
    selected: Boolean,
    unreadCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab.route == ROUTE_ALERTS && unreadCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = InstabookBlue) {
                    Text(
                        text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                        color = Color.White
                    )
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(tab.labelRes),
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(tab.labelRes),
            modifier = Modifier.size(24.dp)
        )
    }
}
