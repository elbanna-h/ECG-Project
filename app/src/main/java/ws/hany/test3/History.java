package ws.hany.test3;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import static java.lang.Integer.parseInt;

public class History extends AppCompatActivity {

    DBH dbH;
    private ListView listView;
    private LineGraphSeries<DataPoint> mSeries;
    String values;
    GraphView graph;
    TextView dateText,bpmText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dateText= findViewById(R.id.date);
        bpmText= findViewById(R.id.bpm);
        listView= findViewById(R.id.listView);

        graph = findViewById(R.id.graph);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getViewport().setScrollableY(true);
        graph.getViewport().setScalable(true);


        dbH = new DBH(this);
        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1200);
        graph.getViewport().setXAxisBoundsManual(true);

        final Cursor cursor = dbH.getAllECGs();
        String [] columns = new String[] {
                DBH.HANY_COLUMN_ID,
                DBH.HANY_COLUMN_DATE
        };
        int [] widgets = new int[] {
                R.id.graphID,
                R.id.graphDate
        };

        final SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this, R.layout.item_info,
                cursor, columns, widgets, 0);

        listView.setAdapter(cursorAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Cursor itemCursor = (Cursor) History.this.listView.getItemAtPosition(position);

            values = itemCursor.getString(itemCursor.getColumnIndex(DBH.HANY_COLUMN_VALUE));
            String BPM = itemCursor.getString(itemCursor.getColumnIndex(DBH.HANY_COLUMN_BPM));
            String date = itemCursor.getString(itemCursor.getColumnIndex(DBH.HANY_COLUMN_DATE));

            Log.d("Debug", values);
            Log.d("Debug", BPM);
            Log.d("Debug", date);

            mSeries.resetData(generateData());
            bpmText.setText(BPM);
            dateText.setText(date);
        });
    }


    private DataPoint[] generateData() {
        String[] parts=values.split(" ");
        int count = parts.length;
        DataPoint[] dataValues = new DataPoint[count];
        for (int x=0; x<count; x++) {
            double y= parseInt(parts[x]);
            DataPoint v = new DataPoint(x, y);
            dataValues[x] = v;
        }
        return dataValues;
    }

}
