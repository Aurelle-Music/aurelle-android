package com.aurelle.music.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Scaffold
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.aurelle.music.R
import com.aurelle.music.data.Album
import com.aurelle.music.data.Artist
import com.aurelle.music.data.MediaItem
import com.aurelle.music.data.Section
import com.aurelle.music.AurelleApplication
import com.aurelle.music.data.Song
import com.aurelle.music.download.DownloadRequest
import com.aurelle.music.download.SongDownloads
import com.aurelle.music.download.forSong
import com.aurelle.music.ui.components.CardDownloads
import com.aurelle.music.ui.download.DownloadSheetHost
import com.aurelle.music.ui.album.AlbumScreen
import com.aurelle.music.ui.album.AlbumViewModel
import com.aurelle.music.ui.artist.ArtistRef
import com.aurelle.music.ui.artist.ArtistAlbumsScreen
import com.aurelle.music.ui.artist.ArtistScreen
import com.aurelle.music.ui.artist.ArtistSongsScreen
import com.aurelle.music.ui.artist.ArtistViewModel
import com.aurelle.music.ui.library.LibraryActions
import com.aurelle.music.ui.library.LibraryAlbumScreen
import com.aurelle.music.ui.library.LibraryAlbumsScreen
import com.aurelle.music.ui.library.LibraryArtistScreen
import com.aurelle.music.ui.library.LibraryArtistsScreen
import com.aurelle.music.ui.library.LibraryScreen
import com.aurelle.music.ui.library.LibrarySongsScreen
import com.aurelle.music.ui.library.LibraryViewModel
import com.aurelle.music.player.PlayerUiState
import com.aurelle.music.player.PlayerViewModel
import com.aurelle.music.ui.player.MiniPlayer
import com.aurelle.music.ui.player.PlayerScreen
import com.aurelle.music.ui.components.AurelleBackdrop
import com.aurelle.music.ui.components.AurelleBottomBar
import com.aurelle.music.ui.components.AurelleSearchBar
import com.aurelle.music.ui.navigation.Routes
import com.aurelle.music.ui.navigation.TopLevelDestination
import com.aurelle.music.ui.screens.HomeScreen
import com.aurelle.music.settings.IconSwitcher
import com.aurelle.music.ui.settings.SettingsScreen
import com.aurelle.music.ui.screens.SeeAllScreen
import com.aurelle.music.ui.search.SearchViewModel

@Composable
fun AurelleApp(openPlayerSignal: Int = 0) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Um único ViewModel (escopo da Activity) compartilhado entre Home e "ver tudo".
    val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()

    // Configurações (aba Conta): o estilo e os padrões vivem no DataStore; a UI só lê o estado atual.
    val settingsRepository = AurelleApplication.settings
    val settings by settingsRepository.state.collectAsStateWithLifecycle()

    // Player: estado compartilhado (mini player + player completo).
    val playerViewModel: PlayerViewModel = viewModel(factory = PlayerViewModel.Factory)
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    val hasTrack = playerState.nowPlaying != null
    LaunchedEffect(hasTrack) { if (!hasTrack) playerExpanded = false }

    // Toque na notificação de mídia: abre o player completo. Com o app fechado, a conexão com o serviço
    // leva um instante para trazer a música; por isso o pedido fica pendente por até 5 s.
    var pendingOpenPlayer by remember { mutableStateOf(false) }
    LaunchedEffect(openPlayerSignal) {
        if (openPlayerSignal > 0) {
            pendingOpenPlayer = true
            delay(5_000)
            pendingOpenPlayer = false
        }
    }
    LaunchedEffect(pendingOpenPlayer, hasTrack) {
        if (pendingOpenPlayer && hasTrack) {
            playerExpanded = true
            pendingOpenPlayer = false
        }
    }

    // Downloads (Etapa 4): estado de todas as músicas; o botão do player usa o da música atual.
    val downloads = AurelleApplication.downloads
    val downloadStatuses by downloads.statuses.collectAsStateWithLifecycle()
    // Música para a qual a folha "Baixar" está aberta (player ou card). Uma folha só, para o app todo.
    var downloadRequest by remember { mutableStateOf<DownloadRequest?>(null) }
    val cardDownloads = CardDownloads(
        statuses = downloadStatuses,
        onOpen = { song ->
            downloadRequest = DownloadRequest(
                id = song.id,
                title = song.title,
                artist = song.artist,
                imageUrl = song.imageUrl,
                durationSeconds = song.durationSeconds,
                album = song.album,
                trackNumber = song.trackNumber,
            )
        },
        onCancel = downloads::cancel,
    )

    val context = LocalContext.current
    // Android 13+: a notificação de mídia precisa da permissão; pedimos uma vez, ao tocar a primeira música.
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    val askNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && !askedNotificationPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotificationPermission = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val onItemClick: (MediaItem, List<MediaItem>) -> Unit = { item, queue ->
        when (item) {
            is Song -> {
                askNotificationPermission()
                playerViewModel.play(queue.filterIsInstance<Song>(), item)
            }
            is Artist -> navController.navigate(Routes.artist(item))
        }
    }

    // Páginas de artista/álbum (online): tocar uma música cria a fila com a lista da página; "Aleatório" embaralha.
    val playOnline: (Song, List<Song>) -> Unit = { song, queue ->
        askNotificationPermission()
        playerViewModel.play(queue, song)
    }
    val shuffleOnline: (List<Song>) -> Unit = { queue ->
        val shuffled = queue.shuffled()
        shuffled.firstOrNull()?.let { playOnline(it, shuffled) }
    }

    // Biblioteca (arquivos baixados): um ViewModel só, compartilhado pela raiz e pelas subtelas.
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val libraryActions = LibraryActions(
        playingId = playerState.nowPlaying?.id,
        onPlay = { track, queue ->
            askNotificationPermission()
            playerViewModel.playLocal(queue, track)
        },
        onShuffle = { queue ->
            val shuffled = queue.shuffled()
            shuffled.firstOrNull()?.let { first ->
                askNotificationPermission()
                playerViewModel.playLocal(shuffled, first)
            }
        },
        onRemove = libraryViewModel::remove,
        onOpenArtist = { name -> navController.navigate(Routes.libraryArtist(name)) },
        onOpenAlbum = { album -> navController.navigate(Routes.libraryAlbum(album.title, album.artist)) },
    )

    // A busca fica só na Home e no "ver tudo"; as páginas de artista/álbum são da aba Home, mas sem a barra de busca.
    val onSearchScreen = currentRoute == Routes.HOME ||
        currentRoute?.startsWith(Routes.SEE_ALL_PREFIX) == true
    val onHomeFlow = onSearchScreen ||
        currentRoute?.startsWith(Routes.ARTIST_PREFIX) == true ||
        currentRoute?.startsWith(Routes.ALBUM_PREFIX) == true
    val onLibraryFlow = currentRoute?.startsWith(Routes.LIBRARY_PREFIX) == true

    val selectedTab = when {
        onHomeFlow -> TopLevelDestination.HOME
        onLibraryFlow -> TopLevelDestination.LIBRARY
        currentRoute == Routes.ACCOUNT -> TopLevelDestination.ACCOUNT
        else -> TopLevelDestination.HOME
    }

    AurelleBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AurelleDock(
                    showSearch = onSearchScreen,
                    query = query,
                    onQueryChange = searchViewModel::onQueryChange,
                    player = playerState,
                    playerProgress = playerViewModel.progress,
                    onOpenPlayer = { playerExpanded = true },
                    onTogglePlay = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                    selectedTab = selectedTab,
                    onSelectTab = { destination ->
                        if (destination == selectedTab) {
                            // Tocar na aba que já está aberta volta para a raiz dela (como no Apple Music).
                            navController.popBackStack(destination.route, inclusive = false)
                        } else {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(350, easing = FastOutSlowInEasing)) { it / 12 }
                },
                exitTransition = {
                    fadeOut(tween(250, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 12 }
                },
                popEnterTransition = {
                    fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(350, easing = FastOutSlowInEasing)) { -it / 12 }
                },
                popExitTransition = {
                    fadeOut(tween(250, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 12 }
                },
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        state = searchState,
                        onOpenSection = { section -> navController.navigate(Routes.seeAll(section)) },
                        onItemClick = onItemClick,
                        onRetry = searchViewModel::retry,
                        downloads = cardDownloads,
                    )
                }
                composable(
                    route = Routes.SEE_ALL_PATTERN,
                    arguments = listOf(navArgument(Routes.SECTION_ARG) { type = NavType.StringType }),
                ) { entry ->
                    val section = Section.valueOf(
                        entry.arguments?.getString(Routes.SECTION_ARG) ?: Section.ARTISTS.name
                    )
                    SeeAllScreen(
                        section = section,
                        state = searchState[section],
                        onBack = { navController.popBackStack() },
                        onItemClick = onItemClick,
                        onLoadMore = { searchViewModel.loadMore(section) },
                        onRetry = searchViewModel::retry,
                        downloads = cardDownloads,
                    )
                }
                // --- Home -> Artista -> Álbum -> Músicas (online) ---------------------------------
                composable(
                    route = Routes.ARTIST_PATTERN,
                    arguments = listOf(
                        stringArg(Routes.ARG_ID),
                        stringArg(Routes.ARG_NAME),
                        stringArg(Routes.ARG_IMAGE),
                        navArgument(Routes.ARG_SUBS) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) { entry ->
                    val artistViewModel = artistViewModelFor(entry)
                    val songs by artistViewModel.songs.collectAsStateWithLifecycle()
                    val albums by artistViewModel.albums.collectAsStateWithLifecycle()
                    val songsPaging by artistViewModel.songsPaging.collectAsStateWithLifecycle()
                    val albumsPaging by artistViewModel.albumsPaging.collectAsStateWithLifecycle()
                    ArtistScreen(
                        ref = artistViewModel.ref,
                        songs = songs,
                        albums = albums,
                        songsHasMore = songsPaging.canLoadMore,
                        albumsHasMore = albumsPaging.canLoadMore,
                        playingId = playerState.nowPlaying?.id,
                        downloads = cardDownloads,
                        onBack = { navController.popBackStack() },
                        onRetry = artistViewModel::retry,
                        onPlay = playOnline,
                        onShuffle = shuffleOnline,
                        onOpenAlbum = { album -> navController.navigate(Routes.album(album)) },
                        onOpenAllSongs = { navController.navigate(Routes.ARTIST_SONGS) },
                        onOpenAllAlbums = { navController.navigate(Routes.ARTIST_ALBUMS) },
                    )
                }
                // "Ver tudo" do artista: mesmo ViewModel da página do artista (a entrada de onde viemos).
                composable(Routes.ARTIST_SONGS) { entry ->
                    val artistEntry = remember(entry) { navController.getBackStackEntry(Routes.ARTIST_PATTERN) }
                    val artistViewModel = artistViewModelFor(artistEntry)
                    val songs by artistViewModel.songs.collectAsStateWithLifecycle()
                    val paging by artistViewModel.songsPaging.collectAsStateWithLifecycle()
                    ArtistSongsScreen(
                        ref = artistViewModel.ref,
                        songs = songs,
                        paging = paging,
                        playingId = playerState.nowPlaying?.id,
                        downloads = cardDownloads,
                        onBack = { navController.popBackStack() },
                        onRetry = artistViewModel::retry,
                        onLoadMore = artistViewModel::loadMoreSongs,
                        onPlay = playOnline,
                        onShuffle = shuffleOnline,
                    )
                }
                composable(Routes.ARTIST_ALBUMS) { entry ->
                    val artistEntry = remember(entry) { navController.getBackStackEntry(Routes.ARTIST_PATTERN) }
                    val artistViewModel = artistViewModelFor(artistEntry)
                    val albums by artistViewModel.albums.collectAsStateWithLifecycle()
                    val paging by artistViewModel.albumsPaging.collectAsStateWithLifecycle()
                    ArtistAlbumsScreen(
                        ref = artistViewModel.ref,
                        albums = albums,
                        paging = paging,
                        onBack = { navController.popBackStack() },
                        onRetry = artistViewModel::retry,
                        onLoadMore = artistViewModel::loadMoreAlbums,
                        onOpenAlbum = { album -> navController.navigate(Routes.album(album)) },
                    )
                }
                composable(
                    route = Routes.ALBUM_PATTERN,
                    arguments = listOf(
                        stringArg(Routes.ARG_ID),
                        stringArg(Routes.ARG_NAME),
                        stringArg(Routes.ARG_IMAGE),
                        stringArg(Routes.ARG_ARTIST),
                        stringArg(Routes.ARG_ARTIST_URL),
                    ),
                ) { entry ->
                    val args = entry.arguments
                    val album = Album(
                        id = Routes.arg(args?.getString(Routes.ARG_ID)).orEmpty(),
                        title = Routes.arg(args?.getString(Routes.ARG_NAME)).orEmpty(),
                        imageUrl = Routes.arg(args?.getString(Routes.ARG_IMAGE)),
                        artist = Routes.arg(args?.getString(Routes.ARG_ARTIST)).orEmpty(),
                        artistUrl = Routes.arg(args?.getString(Routes.ARG_ARTIST_URL)),
                    )
                    val albumViewModel: AlbumViewModel =
                        viewModel(key = "album|${album.id}|${album.title}", factory = AlbumViewModel.factory(album))
                    val detail by albumViewModel.detail.collectAsStateWithLifecycle()
                    AlbumScreen(
                        album = album,
                        detail = detail,
                        playingId = playerState.nowPlaying?.id,
                        downloads = cardDownloads,
                        onBack = { navController.popBackStack() },
                        onRetry = albumViewModel::retry,
                        onPlay = playOnline,
                        onShuffle = shuffleOnline,
                        onOpenArtist = { shown ->
                            // O álbum só conhece o nome e o link do artista; a foto chega quando a página carrega.
                            navController.navigate(
                                Routes.artist(Artist(id = shown.artistUrl.orEmpty(), title = shown.artist, imageUrl = null, subscriberCount = null))
                            )
                        },
                    )
                }

                // --- Biblioteca -> Artistas -> Álbuns -> Músicas (arquivos baixados) --------------
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        actions = libraryActions,
                        onOpenArtists = { navController.navigate(Routes.LIBRARY_ARTISTS) },
                        onOpenAlbums = { navController.navigate(Routes.LIBRARY_ALBUMS) },
                        onOpenSongs = { navController.navigate(Routes.LIBRARY_SONGS) },
                    )
                }
                composable(Routes.LIBRARY_ARTISTS) {
                    LibraryArtistsScreen(libraryViewModel, libraryActions, onBack = { navController.popBackStack() })
                }
                composable(Routes.LIBRARY_ALBUMS) {
                    LibraryAlbumsScreen(libraryViewModel, libraryActions, onBack = { navController.popBackStack() })
                }
                composable(Routes.LIBRARY_SONGS) {
                    LibrarySongsScreen(libraryViewModel, libraryActions, onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.LIBRARY_ARTIST_PATTERN,
                    arguments = listOf(stringArg(Routes.ARG_NAME)),
                ) { entry ->
                    LibraryArtistScreen(
                        name = Routes.arg(entry.arguments?.getString(Routes.ARG_NAME)).orEmpty(),
                        viewModel = libraryViewModel,
                        actions = libraryActions,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.LIBRARY_ALBUM_PATTERN,
                    arguments = listOf(stringArg(Routes.ARG_NAME), stringArg(Routes.ARG_ARTIST)),
                ) { entry ->
                    LibraryAlbumScreen(
                        title = Routes.arg(entry.arguments?.getString(Routes.ARG_NAME)).orEmpty(),
                        artist = Routes.arg(entry.arguments?.getString(Routes.ARG_ARTIST)).orEmpty(),
                        viewModel = libraryViewModel,
                        actions = libraryActions,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ACCOUNT) {
                    SettingsScreen(
                        settings = settings,
                        onChange = settingsRepository::update,
                        onIconChange = { icon ->
                            IconSwitcher.apply(context, icon)
                            settingsRepository.update { it.copy(icon = icon) }
                        },
                    )
                }
            }
        }

        // Player completo por cima de tudo (sobe de baixo; voltar/descer fecha).
        AnimatedVisibility(
            visible = playerExpanded && hasTrack,
            enter = slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(250, easing = FastOutSlowInEasing)),
        ) {
            PlayerScreen(
                state = playerState,
                progress = playerViewModel.progress,
                onClose = { playerExpanded = false },
                onTogglePlay = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onPrevious = playerViewModel::previous,
                onSeek = playerViewModel::seekTo,
                onToggleShuffle = playerViewModel::toggleShuffle,
                onCycleRepeat = playerViewModel::cycleRepeat,
                downloads = playerState.nowPlaying?.let { downloadStatuses.forSong(it.id) } ?: SongDownloads(),
                onOpenDownload = {
                    playerState.nowPlaying?.let { track ->
                        downloadRequest = DownloadRequest(
                            id = track.id,
                            title = track.title,
                            artist = track.artist,
                            imageUrl = track.artworkUrl,
                            durationSeconds = track.durationHintMs?.takeIf { it > 0 }?.div(1000),
                            album = track.album,
                            trackNumber = track.trackNumber,
                        )
                    }
                },
                onCancelDownload = { playerState.nowPlaying?.let { downloads.cancel(it.id) } },
                modifier = Modifier.fillMaxSize(),
            )
        }

        DownloadSheetHost(
            request = downloadRequest,
            statuses = downloadStatuses,
            defaults = downloads.preferences.load(),
            onDownload = { request, choice ->
                downloads.preferences.save(choice)
                downloads.enqueue(request.toJob(choice))
            },
            onDismiss = { downloadRequest = null },
        )
    }
}

/** Dock inferior: busca (só no fluxo da Home) + navegação flutuante. Some a navegação com o teclado aberto. */
@Composable
private fun AurelleDock(
    showSearch: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    player: PlayerUiState,
    playerProgress: kotlinx.coroutines.flow.StateFlow<com.aurelle.music.player.PlaybackProgress>,
    onOpenPlayer: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    selectedTab: TopLevelDestination,
    onSelectTab: (TopLevelDestination) -> Unit,
) {
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(
        Modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
    ) {
        AnimatedVisibility(visible = player.nowPlaying != null && !imeVisible) {
            Column {
                MiniPlayer(
                    state = player,
                    progress = playerProgress,
                    onOpen = onOpenPlayer,
                    onTogglePlay = onTogglePlay,
                    onNext = onNext,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
        AnimatedVisibility(visible = showSearch) {
            Column {
                AurelleSearchBar(query = query, onQueryChange = onQueryChange)
                Spacer(Modifier.height(12.dp))
            }
        }
        AnimatedVisibility(visible = !imeVisible) {
            AurelleBottomBar(selected = selectedTab, onSelect = onSelectTab)
        }
    }
}

/** ViewModel do artista da [entry] (a rota da página do artista); as telas "ver tudo" reaproveitam o mesmo. */
@Composable
private fun artistViewModelFor(entry: NavBackStackEntry): ArtistViewModel {
    val args = entry.arguments
    val ref = ArtistRef(
        id = Routes.arg(args?.getString(Routes.ARG_ID)).orEmpty(),
        name = Routes.arg(args?.getString(Routes.ARG_NAME)).orEmpty(),
        imageUrl = Routes.arg(args?.getString(Routes.ARG_IMAGE)),
        subscribers = args?.getLong(Routes.ARG_SUBS, -1L)?.takeIf { it >= 0 },
    )
    return viewModel(
        viewModelStoreOwner = entry,
        key = "artist|${ref.id}|${ref.name}",
        factory = ArtistViewModel.factory(ref),
    )
}

/** Argumento de texto de uma rota (sempre presente; vazio é mandado como marcador, ver [Routes.arg]). */
private fun stringArg(name: String) = navArgument(name) {
    type = NavType.StringType
    defaultValue = ""
}
