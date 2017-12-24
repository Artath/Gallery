package com.example.gallery

import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_full_screen.*
import java.io.File

class FullScreenActivity : AppCompatActivity() {

    private lateinit var path: String
    lateinit var file: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)
        setSupportActionBar(toolbar)

        path = intent.getStringExtra("photo")
        file = File(path)
        img_view.setImageBitmap(BitmapFactory.decodeFile(path))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_full_screen, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.rename_btn -> {
                    val renameEditText = EditText(this)
                    renameEditText.setText(file.name)
                    val dialog = AlertDialog.Builder(this)
                            .setTitle("Rename Image")
                            .setView(renameEditText)
                            .setMessage("Enter new name")
                            .setPositiveButton("Accept", DialogInterface.OnClickListener { dialog, which ->
                                if (renameEditText.length() != 0) {
                                    var newFile = File(file.parent, renameEditText.text.toString() + "." + file.extension)
                                    file.renameTo(newFile)
                                    Toast.makeText(applicationContext, "New name: " + newFile.name, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(applicationContext, "Enter the link to the jpeg format image", Toast.LENGTH_SHORT).show()
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create()
                    dialog.show()
                }
                R.id.share_btn -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "image/"
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                    startActivity(Intent.createChooser(shareIntent, "Share image using"))
                }
                R.id.delete_btn -> {
                    var fileName = file.name
                    var deleted = file.delete()
                    if (deleted){
                        Toast.makeText(applicationContext, "File $fileName deleted", Toast.LENGTH_SHORT).show()
                        var intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                    }else{
                        Toast.makeText(applicationContext, "Something went wrong", Toast.LENGTH_SHORT)
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
