package com.bulifier.core.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.bulifier.core.db.Content
import com.bulifier.core.db.File
import com.bulifier.core.db.FileData
import com.bulifier.core.db.Project
import com.bulifier.core.db.db
import com.bulifier.core.prefs.Prefs
import com.bulifier.core.prefs.Prefs.path
import com.bulifier.core.prefs.Prefs.projectId
import com.bulifier.core.prefs.Prefs.projectName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FullPath(
    val content: FileData?,
    val path: String
)

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    private val db by lazy { app.db.fileDao() }

    val projectsFlow = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            maxSize = 100
        )
    ) {
        db.fetchProjects()
    }.flow.cachedIn(viewModelScope)

    val openedFile: LiveData<FileData?> = MutableLiveData()
    val fullPath = MediatorLiveData<FullPath>().apply {
        addSource(openedFile) {
            value = kotlin.run {
                val value = path.value
                FullPath(it, extractPath(value))
            }
        }
        addSource(path) {
            value = FullPath(openedFile.value, extractPath(it))
        }
    }

    private fun extractPath(value: String?): String {
        val path = if (!value.isNullOrBlank()) {
            "/$value"
        } else {
            ""
        }
        return "${projectName.value}$path"
    }

    val pagingDataFlow by lazy {
        MediatorLiveData<PagingData<File>>().apply {
            addSource(path) { newPath ->
                val currentProjectId = projectId.value ?: return@addSource
                updatePagingData(newPath, currentProjectId)
            }

            addSource(projectId) { newProjectId ->
                val currentPath = path.value ?: projectName.value ?: return@addSource
                updatePagingData(currentPath, newProjectId)
            }
        }
    }

    fun openFile(file: File) {
        viewModelScope.launch {
            val content = db.getContent(file.fileId) ?: FileData(
                fileId = file.fileId,
                fileName = "",
                path = path.value ?: "",
                content = "",
                type = Content.Type.NONE
            )
            openedFile as MutableLiveData
            openedFile.value = content
        }
    }

    fun closeFile() {
        openedFile as MutableLiveData
        openedFile.value = null
    }

    private fun updatePagingData(currentPath: String, currentProjectId: Long) {
        viewModelScope.launch {
            Pager(
                config = PagingConfig(pageSize = 20),
                pagingSourceFactory = {
                    app.db.fileDao().fetchFilesByPathAndProjectId(
                        currentPath,
                        currentProjectId
                    )
                }
            ).flow.cachedIn(viewModelScope).collect {
                pagingDataFlow.value = it
            }
        }
    }

    suspend fun createProject(projectName: String) {
        val projectId = db.fetchProjects(Project(projectName = projectName))
        withContext(Dispatchers.Main) {
            Prefs.projectId.set(projectId)
            Prefs.projectName.set(projectName)
            path.set("")
        }
    }

    suspend fun selectProject(project:Project) {
        withContext(Dispatchers.Main) {
            projectId.set(project.projectId)
            projectName.set(project.projectName)
            path.set("")
        }
    }

    fun updatePath(path: String) {
        Prefs.path.set(path)
    }

    fun updateCreateFile(fileName: String) {
        viewModelScope.launch {
            val projectId = projectId.value ?: return@launch
            val path = path.value ?: projectName.value ?: return@launch
            db.insertFileAndUpdateParent(
                File(
                    fileName = fileName,
                    projectId = projectId,
                    isFile = true,
                    path = path
                )
            )
        }
    }

    fun createFolder(folderName: String) {
        viewModelScope.launch {
            val projectId = projectId.value ?: return@launch
            val path = path.value ?: return@launch
            db.insertFileAndUpdateParent(
                File(
                    fileName = folderName,
                    projectId = projectId,
                    isFile = false,
                    path = path
                )
            )
        }
    }

    fun updateFileContent(content: String) {
        val openedFile = openedFile.value ?: return
        viewModelScope.launch {
            db.insertContentAndUpdateFileSize(Content(
                fileId = openedFile.fileId,
                content = content
            ))
        }
    }

    fun shareFiles() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val projectId = projectId.value ?: return@withContext
            val files = db.fetchFilesListByProjectId(projectId)
            MultiFileSharingUtil(app).shareFiles(files)
        }
    }

    fun deleteProject(project: Project) = viewModelScope.launch{
        db.deleteProject(project)
    }


}