package com.gcancino.levelingup.presentation.player.dailyTasks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gcancino.levelingup.R
import com.gcancino.levelingup.core.Resource
import com.gcancino.levelingup.domain.models.QuestionBank
import com.gcancino.levelingup.domain.models.dailyTasks.WeeklyRecap
import com.gcancino.levelingup.presentation.player.dailyTasks.viewModels.WeeklyFlowViewModel
import com.gcancino.levelingup.ui.theme.BackgroundColor
import com.gcancino.levelingup.ui.theme.SystemColors.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyFlowScreen(
    viewModel: WeeklyFlowViewModel = hiltViewModel(),
    onCompleted: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val recapResource by viewModel.recapData.collectAsStateWithLifecycle()
    
    LaunchedEffect(currentStep) {
        if (currentStep == WeeklyFlowViewModel.WeeklyFlowStep.Completed) {
            onCompleted()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("WEEKLY RESET", fontWeight = FontWeight.Bold, letterSpacing = 2.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.Unspecified
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .padding(padding)
        ) {
            LinearProgressIndicator(
                progress = {
                    when (currentStep) {
                        WeeklyFlowViewModel.WeeklyFlowStep.Recap -> 0.33f
                        WeeklyFlowViewModel.WeeklyFlowStep.Reflect -> 0.66f
                        else -> 1f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryColor,
                trackColor = Color.DarkGray
            )

            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    WeeklyFlowViewModel.WeeklyFlowStep.Recap -> RecapStep(recapResource)
                    WeeklyFlowViewModel.WeeklyFlowStep.Reflect -> ReflectStep(viewModel)
                    WeeklyFlowViewModel.WeeklyFlowStep.Architect -> ArchitectStep(viewModel)
                    else -> Unit
                }
            }

            Button(
                onClick = { 
                    if (currentStep == WeeklyFlowViewModel.WeeklyFlowStep.Architect) viewModel.saveWeeklyReset()
                    else viewModel.nextStep() 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text(
                    text = if (currentStep == WeeklyFlowViewModel.WeeklyFlowStep.Architect) "SAVE RESET" else "CONTINUE",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun RecapStep(recapResource: Resource<WeeklyRecap>) {
    when (recapResource) {
        is Resource.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        }
        is Resource.Success -> {
            val recap = recapResource.data!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("The Week in Review", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Identity Alignment Score: ${(recap.identityAlignmentScore * 100).toInt()}%", color = PrimaryColor)
                }
                
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Wins & Integration", fontWeight = FontWeight.Bold, color = Color.White)
                            if (recap.dailyWins.isEmpty()) {
                                Text("No specific wins recorded yet. Consistency is the primary win.", color = Color.Gray)
                            }
                        }
                    }
                }
                
                item {
                    Text("Objectives Status", fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                items(recap.completedObjectives) { obj ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "Completed", tint = Color.Green, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(obj.title, color = Color.White)
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
fun ReflectStep(viewModel: WeeklyFlowViewModel) {
    val questions = remember { QuestionBank.getTodaysWeeklyQuestions() }
    val answers by viewModel.answers.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Neuro-Alignment", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Rewiring your identity through deep reflection.", color = Color.Gray)
        }

        items(questions) { q ->
            val text = answers.find { it.questionId == q.id }?.answer ?: ""
            Column {
                Text(stringResource(q.textRes), color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = text,
                    onValueChange = { viewModel.updateAnswer(q.id, it, "Text for ${q.id}") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.DarkGray,
                        focusedContainerColor = Color(0xFF333333),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    placeholder = { Text("Write your thoughts...", color = Color.Gray) }
                )
            }
        }
    }
}

@Composable
fun ArchitectStep(viewModel: WeeklyFlowViewModel) {
    val nextObjectives by viewModel.nextWeekObjectives.collectAsStateWithLifecycle()
    var newObjTitle by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Architect Next Week", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("What will the 100% version of you achieve?", color = Color.Gray)
        }

        item {
            OutlinedTextField(
                value = newObjTitle,
                onValueChange = { newObjTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("New Weekly Objective") },
                trailingIcon = {
                    IconButton(onClick = { 
                        if (newObjTitle.isNotBlank()) {
                            viewModel.addObjective(newObjTitle, "athlete") // Defaulting for now
                            newObjTitle = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryColor
                )
            )
        }

        items(nextObjectives) { obj ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = PrimaryColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(obj.title, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
