package com.example.vetfinance.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vetfinance.R
import com.example.vetfinance.data.PetWithOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vetfinance.viewmodel.PetViewModel

@Composable
fun PetsScreen(
    navController: NavController,
    viewModel: PetViewModel = hiltViewModel()
) {

    val petsWithOwners by viewModel.filteredPetsWithOwners.collectAsStateWithLifecycle()
    val searchQuery by viewModel.petSearchQuery.collectAsStateWithLifecycle()


    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPetSearchQuery()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_pet_screen") }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_pet_content_description))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.pets_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )


            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onPetSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_pet_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearPetSearchQuery() }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search_content_description))
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))


            if (petsWithOwners.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_pets_found))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(petsWithOwners) { petWithOwner ->
                        PetItem(
                            petWithOwner = petWithOwner,
                            onItemClick = {
                                navController.navigate("pet_detail/${petWithOwner.pet.petId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PetItem(petWithOwner: PetWithOwner, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = petWithOwner.pet.name,
                fontWeight = FontWeight.Bold
            )
            Text(text = stringResource(R.string.owner_label, petWithOwner.owner.name))
        }
    }
}
