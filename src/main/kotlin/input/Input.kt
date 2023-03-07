package input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import upload.UploadDialog

@Composable
fun Input(onUrl: (String) -> Unit) {

    val (url, setUrl) = remember { mutableStateOf("") }

    val (uploadState, setUploadState) = remember { mutableStateOf(false) }

    if (uploadState) UploadDialog(setUrl)

    DisposableEffect(uploadState) {
        if (!uploadState && url.isNotBlank()) onUrl(url)
        onDispose { setUploadState(false) }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextField(url, setUrl, modifier = Modifier.weight(1f), trailingIcon = {
                if (url.isNotBlank()) IconButton(onClick = {
                    setUrl("")
                }) {
                    Icon(Icons.Rounded.Clear, "clear url")
                }
            })
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUrl(url) }, enabled = url.isNotBlank()) {
                    Icon(Icons.Rounded.Done, "done")
                }
                IconButton(onClick = { setUploadState(true) }) {
                    Icon(Icons.Rounded.FileUpload, "upload")
                }
            }
        }
    }
}