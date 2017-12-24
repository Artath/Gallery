package com.example.gallery

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.view.ActionMode
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*


open class MainActivity : AppCompatActivity() {

    private var actionMode: ActionMode? = null
    private lateinit var listOfPhotoFiles: ArrayList<File>
    private lateinit var adapter: Adapter
    private val CAMERA_REQUEST_CODE = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        populateRecyclerView()
        implementRecyclerViewClickListeners()
    }

    private fun populateRecyclerView(){
        listOfPhotoFiles = getListOfPhotos("")
        adapter = Adapter(listOfPhotoFiles, applicationContext)
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter
        recyclerView.adapter.notifyDataSetChanged()
    }

    private fun getFiles(dir: File): ArrayList<File> {
        val fileObj = dir.listFiles()
        var list = ArrayList<File>()
        for (f: File in fileObj) {
            if (f.isDirectory) {
                list.addAll(getFiles(f))
            } else if (f.name.endsWith(".jpg") || f.name.endsWith(".jpeg") || f.name.endsWith(".png")) {
                    list.add(f)
            }
        }
        return list
    }

    private fun getListOfPhotos(option: String): ArrayList<File> {
        val dcimDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        var listFiles = getFiles(dcimDirectory)
        listFiles.addAll(getFiles(picturesDirectory))
        if(option == "all"){
            val exStorageDir = Environment.getExternalStorageDirectory()
            listFiles.addAll(getFiles(exStorageDir))
        }

        var sortedList = listFiles.sortedWith(kotlin.Comparator(
                fun(a: File, b: File): Int {
                    return b.lastModified().compareTo(a.lastModified())
                }
            ))
        return ArrayList(sortedList)
    }

    private fun implementRecyclerViewClickListeners() {
        recyclerView.addOnItemTouchListener(
                RecyclerTouchListener(applicationContext, recyclerView,
                object : RecyclerClickListener {
                    override fun onClick(view: View, position: Int) {
                        if (actionMode != null) {
                            onListItemSelect(position)
                        } else {
                            val intent = Intent(applicationContext, FullScreenActivity::class.java)
                            intent.putExtra("photo", adapter.data[position].absolutePath)
                            startActivity(intent)
                        }
                    }
                    override fun onLongClick(view: View, position: Int) {
                        onListItemSelect(position)
                    }
                }))
    }

    private fun onListItemSelect(position: Int) {
        adapter.toggleSelection(position)
        val hasCheckedItems = adapter.getSelectedCount() > 0
        if (hasCheckedItems && actionMode == null)
            actionMode = startSupportActionMode(ToolbarActionModeCallback(applicationContext, adapter, listOfPhotoFiles))
        else if (!hasCheckedItems && actionMode != null) {
            actionMode?.finish()
        }
    }

    fun setNullToActionMode() {
        if (actionMode != null)
            actionMode = null
    }

    fun deleteRows() {
        var selected = adapter.getSelectedIds()
        for (i in selected.size() downTo 0) {
            if (selected.valueAt(i)) {
                listOfPhotoFiles[selected.keyAt(i)].delete()
                adapter.notifyDataSetChanged()
            }
        }
        Toast.makeText(applicationContext,selected.size().toString() +  " items deleted.", Toast.LENGTH_SHORT).show()
        actionMode?.finish()
        populateRecyclerView()
    }

    fun shareSeveralPhotos(){
        var selected = adapter.getSelectedIds()
        val filesToSend = ArrayList<Uri>()
        for(i in selected.size() downTo 1){
            val file = File(listOfPhotoFiles[selected.keyAt(i)].path)
            val uri = Uri.fromFile(file)
            filesToSend.add(uri)
        }
        val intent = Intent()
        intent.action = Intent.ACTION_SEND_MULTIPLE
        intent.type = "image/jpeg"
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, filesToSend)
        startActivity(Intent.createChooser(intent, "Share images using"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_option, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.camera_btn -> {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (cameraIntent.resolveActivity(packageManager) != null) {
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
                    }
                }
                R.id.loadUrl_btn -> {
                    val taskEditText = EditText(this)
                    val dialog = AlertDialog.Builder(this)
                            .setTitle("Download Image by URL")
                            .setView(taskEditText)
                            .setMessage("What do you want to do next?")
                            .setPositiveButton("Add", DialogInterface.OnClickListener { dialog, which ->
                                var link = taskEditText.text.toString()
                                if (taskEditText.length() != 0 || link.endsWith(".jpg") ) {
                                    var task = MyTask()
                                    task.execute(link)
                                } else {
                                    toastMessage("Enter the link to the jpeg format image")
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                    dialog.show()
                }
                R.id.action_searh -> {
                    listOfPhotoFiles = getListOfPhotos("all")
                    adapter = Adapter(listOfPhotoFiles, applicationContext)

                    populateRecyclerView()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            CAMERA_REQUEST_CODE -> {
                if(resultCode == Activity.RESULT_OK && data != null){
                    var photo = data.extras.get("data") as Bitmap
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "IMG_" + Calendar.getInstance().time + ".jpg")
                    val outputStream = FileOutputStream(file)
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    populateRecyclerView()
                }
            }
            else -> {
                Toast.makeText(applicationContext, "Unrecognized request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    inner class MyTask:AsyncTask<String, Void, Bitmap>(){
        override fun doInBackground(vararg p0: String?): Bitmap {
            var name = "IMG_" + Calendar.getInstance().time + ".jpg"
            var file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), name)
            file.createNewFile()
            var fos = FileOutputStream(file)
            var link = URL(p0[0])
            fos.write(link.readBytes())
            fos.close()
            return BitmapFactory.decodeFile(file.absolutePath)
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
        }
    }

    inner class ToolbarActionModeCallback(private val context: Context,
                                    private val recyclerView_adapter: Adapter,
                                    private val files: ArrayList<File>) : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_main, menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.findItem(R.id.action_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.findItem(R.id.action_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_delete ->{
                    deleteRows()
                }
                R.id.action_share -> {
                    shareSeveralPhotos()
                    mode.finish()
                }
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            recyclerView_adapter.removeSelection()
            setNullToActionMode()
        }
    }

}
