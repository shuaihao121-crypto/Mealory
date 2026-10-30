package com.mealory.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.mealory.app.ui.addmeal.AddMealMode
import com.mealory.app.ui.addmeal.AddMealScreen
import com.mealory.app.ui.calendar.CalendarScreen
import com.mealory.app.ui.calendar.DateDetailScreen
import com.mealory.app.ui.components.MealoryLogo
import com.mealory.app.ui.home.HomeScreen
import com.mealory.app.ui.memories.MemoriesScreen
import com.mealory.app.ui.memories.AnnualMemoryScreen
import com.mealory.app.ui.mealdetail.MealDetailScreen
import com.mealory.app.ui.profile.ProfileScreen
import com.mealory.app.ui.privacy.CurrentPrivacyPolicyVersion
import com.mealory.app.ui.privacy.PrivacyConsentScreen
import com.mealory.app.ui.privacy.PrivacyPolicyScreen
import com.mealory.app.ui.deleted.RecentlyDeletedScreen
import com.mealory.app.ui.backup.BackupScreen
import com.mealory.app.ui.settings.SettingsScreen
import com.mealory.app.ui.tags.PeopleScreen
import com.mealory.app.ui.tags.PersonDetailScreen
import com.mealory.app.ui.tags.PlaceDetailScreen
import com.mealory.app.ui.tags.PlacesScreen
import kotlinx.coroutines.delay

private const val SplashRoute = "splash"
private const val PrivacyConsentRoute = "privacy-consent"
private const val OnboardingRoute = "onboarding"
private const val MainRoute = "main"
private const val AddMealRoute = "add-meal"
private const val MealDetailRoute = "meal-detail"
private const val DateDetailRoute = "date-detail"

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    TopLevelDestination("home", "首页", Icons.Rounded.Home),
    TopLevelDestination("calendar", "日历", Icons.Rounded.CalendarMonth),
    TopLevelDestination("memories", "回忆", Icons.Rounded.AutoAwesome),
    TopLevelDestination("profile", "我的", Icons.Rounded.PersonOutline),
)

@Composable
fun MealoryApp(
    onPrivacyDeclined: () -> Unit,
    viewModel: MealoryViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSavedMessage by rememberSaveable { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = SplashRoute) {
        composable(SplashRoute) {
            SplashScreen(
                ready = settings != null,
                onFinished = {
                    val currentSettings = settings ?: return@SplashScreen
                    val destination = when {
                        currentSettings.privacyPolicyVersionAccepted < CurrentPrivacyPolicyVersion -> PrivacyConsentRoute
                        currentSettings.onboardingCompleted -> MainRoute
                        else -> OnboardingRoute
                    }
                    navController.navigate(destination) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
            )
        }
        composable(PrivacyConsentRoute) {
            PrivacyConsentScreen(
                onAccept = {
                    viewModel.acceptPrivacyPolicy(CurrentPrivacyPolicyVersion)
                    val destination = if (settings?.onboardingCompleted == true) MainRoute else OnboardingRoute
                    navController.navigate(destination) {
                        popUpTo(PrivacyConsentRoute) { inclusive = true }
                    }
                },
                onDecline = onPrivacyDeclined,
            )
        }
        composable(OnboardingRoute) {
            OnboardingScreen(
                onContinue = {
                    viewModel.completeOnboarding()
                    navController.navigate(MainRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
            )
        }
        composable(MainRoute) {
            MainShell(
                showSavedMessage = showSavedMessage,
                onSavedMessageShown = { showSavedMessage = false },
                onAddMeal = { mode -> navController.navigate("$AddMealRoute/${mode.name}") },
                onMealClick = { mealId -> navController.navigate("$MealDetailRoute/$mealId") },
                onOpenDay = { epochDay -> navController.navigate("$DateDetailRoute/$epochDay") },
            )
        }
        composable(
            route = "$AddMealRoute/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) {
            AddMealScreen(
                onCancel = { navController.popBackStack() },
                onSaved = {
                    showSavedMessage = true
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = "$MealDetailRoute/{mealId}",
            arguments = listOf(navArgument("mealId") { type = NavType.StringType }),
        ) {
            MealDetailScreen(
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable(
            route = "$DateDetailRoute/{epochDay}",
            arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
        ) {
            DateDetailScreen(
                onBack = { navController.popBackStack() },
                onMealClick = { mealId -> navController.navigate("$MealDetailRoute/$mealId") },
            )
        }
    }
}

@Composable
private fun SplashScreen(
    ready: Boolean,
    onFinished: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(ready) {
        visible = true
        if (ready) {
            delay(1_150)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(650)),
            exit = fadeOut(tween(250)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MealoryLogo(size = 144.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Mealory",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "MEALS TODAY, MEMORIES TOMORROW",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.1.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 44.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        MealoryLogo(size = 88.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Mealory",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = "把吃过的每一顿饭，\n留给未来的自己。",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "记录照片、日期、地点，\n还有和你一起吃饭的人。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "所有数据只保存在你的设备中。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onContinue,
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("开始记录")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    showSavedMessage: Boolean,
    onSavedMessageShown: () -> Unit,
    onAddMeal: (AddMealMode) -> Unit,
    onMealClick: (String) -> Unit,
    onOpenDay: (Long) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: destinations.first().route
    var showAddMealSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            snackbarHostState.showSnackbar("这一顿，记下了。")
            onSavedMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = { showAddMealSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "记录一顿饭", modifier = Modifier.size(28.dp))
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    onAddMeal = { showAddMealSheet = true },
                    onTakePhoto = { onAddMeal(AddMealMode.CAMERA) },
                    onPickPhotos = { onAddMeal(AddMealMode.PICKER) },
                    onNoPhoto = { onAddMeal(AddMealMode.NO_PHOTO) },
                    onMealClick = onMealClick,
                    showAddMealSheet = showAddMealSheet,
                    onDismissAddMealSheet = { showAddMealSheet = false },
                )
            }
            composable("calendar") {
                CalendarScreen(
                    onMealClick = onMealClick,
                    onOpenDay = onOpenDay,
                )
            }
            composable("memories") {
                MemoriesScreen(
                    onMealClick = onMealClick,
                    onAnnualClick = { navController.navigate("annual/$it") },
                )
            }
            composable(
                route = "annual/{year}",
                arguments = listOf(navArgument("year") { type = NavType.IntType }),
            ) {
                AnnualMemoryScreen(
                    onBack = { navController.popBackStack() },
                    onMealClick = onMealClick,
                )
            }
            composable("profile") {
                ProfileScreen(
                    onPeople = { navController.navigate("people") },
                    onPlaces = { navController.navigate("places") },
                    onRecentlyDeleted = { navController.navigate("recently-deleted") },
                    onBackup = { navController.navigate("backup") },
                    onSettings = { navController.navigate("settings") },
                )
            }
            composable("recently-deleted") {
                RecentlyDeletedScreen(onBack = { navController.popBackStack() })
            }
            composable("backup") {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onPrivacyPolicy = { navController.navigate("privacy-policy") },
                )
            }
            composable("privacy-policy") {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
            composable("people") {
                PeopleScreen(
                    onBack = { navController.popBackStack() },
                    onPersonClick = { navController.navigate("person/$it") },
                )
            }
            composable(
                route = "person/{personId}",
                arguments = listOf(navArgument("personId") { type = NavType.StringType }),
            ) {
                PersonDetailScreen(
                    onBack = { navController.popBackStack() },
                    onMealClick = onMealClick,
                )
            }
            composable("places") {
                PlacesScreen(
                    onBack = { navController.popBackStack() },
                    onPlaceClick = { navController.navigate("place/$it") },
                )
            }
            composable(
                route = "place/{placeId}",
                arguments = listOf(navArgument("placeId") { type = NavType.StringType }),
            ) {
                PlaceDetailScreen(
                    onBack = { navController.popBackStack() },
                    onMealClick = onMealClick,
                )
            }
        }
    }
}

