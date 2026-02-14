package net.canvoki.carburoid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.MainSharedViewModel
import net.canvoki.carburoid.R
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.repository.GasStationRepository
import net.canvoki.carburoid.repository.RepositoryEvent
import net.canvoki.carburoid.ui.AppScaffold
import net.canvoki.carburoid.ui.Selectors
import net.canvoki.carburoid.ui.StationList
import net.canvoki.shared.usermessage.UserMessage
import net.canvoki.shared.nolog as log

@Composable
fun StationListScreen(
    viewModel: MainSharedViewModel,
    repository: GasStationRepository,
    onStationClicked: (GasStation) -> Unit,
    onPlotNavigatorClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var isDownloading by remember { mutableStateOf(repository.isFetchInProgress()) }
    var isProcessing by remember { mutableStateOf(viewModel.isProcessingStations) }
    var stations by remember { mutableStateOf<List<GasStation>>(viewModel.getStationsToDisplay()) }
    val failedDownloadFormat = stringResource(R.string.failed_download)

    LaunchedEffect(repository) {
        repository.events.collect { event ->
            isDownloading = repository.isFetchInProgress()
            when (event) {
                is RepositoryEvent.UpdateStarted -> {
                    log("REPO EVENT UpdateStarted")
                }
                is RepositoryEvent.UpdateReady -> {
                    log("REPO EVENT UpdateReady")
                }
                is RepositoryEvent.UpdateFailed -> {
                    log("REPO EVENT UpdateFailed")
                    UserMessage.Info(failedDownloadFormat.format(event.error)).post()
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.stationsReloadStarted.collect {
            isProcessing = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.stationsUpdated.collect { updatedStations ->
            stations = updatedStations
            isProcessing = false
        }
    }

    AppScaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Carburoid") },
                actions = {
                    IconButton(onClick = onPlotNavigatorClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_show_chart),
                            contentDescription = stringResource(R.string.menu_chart),
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            contentDescription = stringResource(R.string.menu_settings),
                            painter = painterResource(R.drawable.ic_settings),
                        )
                    }
                },
            )
        },
    ) {
        Selectors()
        StationList(
            stations = stations,
            downloading = isDownloading,
            processing = isProcessing,
            onRefresh = { repository.launchFetch() },
            onStationClicked = onStationClicked,
            modifier = Modifier.weight(1f),
        )
    }
}
