package jp.techacademy.shirou.ogata.taskapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.app.AlarmManager
import android.app.PendingIntent
import io.realm.RealmQuery
import kotlinx.android.synthetic.main.content_input.*

const val EXTRA_TASK = "jp.techacademy.shirou.ogata.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    //Realm(後で初期化)
    private lateinit var mRealm: Realm
    private var mCategory:String = ""

    //DBに更新があった際は表示も更新する
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    //RealmとListViewをつなぐアダプタ
    private lateinit var mTaskAdapter: TaskAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //タスク追加画面に遷移する
        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        //イベントリスナ設定
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        // ListViewをタップしたときの処理
        listView1.setOnItemClickListener { parent, _, position, _ ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)

            //どの項目が選択されていたかを渡す
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, _, position, _ ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->

                //選択されているidと同じものを検索
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                //トランザクション開始
                mRealm.beginTransaction()

                //削除
                results.deleteAllFromRealm()

                //トランザクション終了
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)

                //表示を更新
                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()

            //ダイアログの表示
            dialog.show()

            true
        }

        //検索
        button1.setOnClickListener{ view ->

            mCategory = editText.text.toString()
            reloadListView()

        }

        //リストビューの更新
        reloadListView()
    }

    private fun reloadListView() {
        // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得

        if(mCategory.length > 0){
            mTaskAdapter.taskList = mRealm.copyFromRealm(mRealm.where(Task::class.java).equalTo("category",mCategory).findAll().sort("date", Sort.DESCENDING))
        }else {
            mTaskAdapter.taskList = mRealm.copyFromRealm(mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING))
        }


        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }
}