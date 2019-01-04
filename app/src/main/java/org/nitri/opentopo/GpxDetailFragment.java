package org.nitri.opentopo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.nitri.opentopo.adapter.WayPointListAdapter;
import org.nitri.opentopo.domain.DistancePoint;
import org.nitri.opentopo.model.WayPointHeaderItem;
import org.nitri.opentopo.model.WayPointItem;
import org.nitri.opentopo.model.WayPointListItem;
import org.nitri.opentopo.view.ChartValueMarkerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.ticofab.androidgpxparser.parser.domain.Gpx;
import io.ticofab.androidgpxparser.parser.domain.Track;
import io.ticofab.androidgpxparser.parser.domain.TrackPoint;
import io.ticofab.androidgpxparser.parser.domain.TrackSegment;
import io.ticofab.androidgpxparser.parser.domain.WayPoint;

public class GpxDetailFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private Gpx mGpx;
    private List<DistancePoint> mTrackDistanceLine;
    private double mDistance;
    private boolean mElevation;
    private LineChart mElevationChart;
    private TextView tvName;
    private TextView tvDescription;
    private TextView tvLength;
    private Typeface mTfRegular;
    private Typeface mTfLight;

    private double mMinElevation = 0;
    private double mMaxElevation = 0;
    private RecyclerView mWayPointRecyclerView;
    List<WayPointListItem> mWayPointListItems = new ArrayList<>();
    private WayPointListAdapter mWayPointListAdapter;


    public GpxDetailFragment() {
        // Required empty public constructor
    }


    public static GpxDetailFragment newInstance() {
        GpxDetailFragment fragment = new GpxDetailFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        mGpx = mListener.getGpx();
        if (mGpx != null && mGpx.getTracks() != null) {
            for (Track track : mGpx.getTracks()) {
                buildTrackDistanceLine(track);
            }
            if (getActivity() != null) {
                mTfRegular = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Regular.ttf");
                mTfLight = Typeface.createFromAsset(getActivity().getAssets(), "OpenSans-Light.ttf");
            }
        }
        mWayPointListAdapter = new WayPointListAdapter(mWayPointListItems);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gpx_detail, container, false);
        tvName = rootView.findViewById(R.id.tvName);
        tvDescription = rootView.findViewById(R.id.tvDescription);
        tvLength = rootView.findViewById(R.id.tvLength);
        ConstraintLayout chartContainer = rootView.findViewById(R.id.chartContainer);
        mElevationChart = rootView.findViewById(R.id.elevationChart);
        mWayPointRecyclerView = rootView.findViewById(R.id.way_point_recycler_view);
        mWayPointRecyclerView.setHasFixedSize(true);
        mWayPointRecyclerView.setNestedScrollingEnabled(false);
        mWayPointRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mWayPointRecyclerView.setAdapter(mWayPointListAdapter);


        if (mElevation) {
            setUpElevationChart();

            setData();

        } else {
            chartContainer.setVisibility(View.GONE);

        }
        return rootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // For now, use title and description of first track
        if (mGpx != null && mGpx.getTracks() != null && mGpx.getTracks().get(0) != null) {
            if (TextUtils.isEmpty(mGpx.getTracks().get(0).getTrackName())) {
                tvName.setVisibility(View.GONE);
            } else {
                tvName.setText(mGpx.getTracks().get(0).getTrackName());
            }
            if (TextUtils.isEmpty(mGpx.getTracks().get(0).getTrackDesc())) {
                tvDescription.setVisibility(View.GONE);
            } else {
                tvDescription.setText(mGpx.getTracks().get(0).getTrackDesc());
            }
        }

        if (mDistance > 0) {
            tvLength.setText(String.format(Locale.getDefault(), "%.2f km", mDistance / 1000f));
        } else {
            tvLength.setVisibility(View.GONE);
        }

        if (mGpx != null && mGpx.getWayPoints() != null) {
            buildWayPointList();
            mWayPointListAdapter.notifyDataSetChanged();
        }
    }

    private void buildTrackDistanceLine(Track track) {
        mTrackDistanceLine = new ArrayList<>();
        mDistance = 0;
        mElevation = false;
        TrackPoint prevTrackPoint = null;
        if (track.getTrackSegments() != null) {
            for (TrackSegment segment : track.getTrackSegments()) {
                if (segment.getTrackPoints() != null) {
                    mMinElevation = mMaxElevation = segment.getTrackPoints().get(0).getElevation();
                    for (TrackPoint trackPoint : segment.getTrackPoints()) {
                        if (prevTrackPoint != null) {
                            DistancePoint.Builder builder = new DistancePoint.Builder();
                            mDistance += Util.distance(prevTrackPoint, trackPoint);
                            builder.setDistance(mDistance);
                            if (trackPoint.getElevation() != null) {
                                double elevation = trackPoint.getElevation();
                                if (elevation < mMinElevation)
                                    mMinElevation = elevation;
                                if (elevation > mMaxElevation)
                                    mMaxElevation = elevation;
                                builder.setElevation(elevation);
                                mElevation = true;
                            }
                            mTrackDistanceLine.add(builder.build());
                        }
                        prevTrackPoint = trackPoint;
                    }
                }
            }
        }
    }

    private void buildWayPointList() {
        String defaultType = getString(R.string.poi);
        List<WayPoint> wayPoints;
        mWayPointListItems.clear();
        for (String type : Util.getWayPointTypes(mGpx, defaultType)) {
            if (type.equals(defaultType)) {
                wayPoints = Util.getWayPointsByType(mGpx, null);
            } else {
                wayPoints = Util.getWayPointsByType(mGpx, type);
            }
            if (wayPoints.size() > 0) {
                mWayPointListItems.add(new WayPointHeaderItem(type));
                for (WayPoint wayPoint : wayPoints) {
                    mWayPointListItems.add(new WayPointItem(wayPoint));
                }
            }
        }
    }

    private void setUpElevationChart() {
        Legend l = mElevationChart.getLegend();
        l.setEnabled(false);
        mElevationChart.getDescription().setEnabled(false);

        ChartValueMarkerView mv = new ChartValueMarkerView(getActivity(), R.layout.chart_value_marker_view);
        mv.setChartView(mElevationChart);
        mElevationChart.setMarker(mv);

        int primaryTextColorInt = Util.resolveColorAttr(getActivity(), android.R.attr.textColorPrimary);

        XAxis xAxis = mElevationChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(primaryTextColorInt);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return String.valueOf((int) (value / 1000));
            }
        });

        YAxis yAxis = mElevationChart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setTypeface(mTfLight);
        yAxis.setDrawGridLines(false);
        yAxis.setGranularityEnabled(true);
        yAxis.setTextColor(primaryTextColorInt);

        float margin = (float) mMaxElevation * .2f;
        float yMin = (float) mMinElevation - margin;
        float yMax = (float) mMaxElevation + margin;
        if (yMin < 0 && mMinElevation >= 0)
            yMin = 0;

        yAxis.setAxisMinimum(yMin);
        yAxis.setAxisMaximum(yMax);

        mElevationChart.getAxisRight().setDrawLabels(false);

    }

    private void setData() {
        ArrayList<Entry> elevationValues = new ArrayList<>();
        for (DistancePoint point : mTrackDistanceLine) {
            elevationValues.add(new Entry((float) point.getDistance(), (float) point.getElevation()));
        }
        LineDataSet elevationDataSet = new LineDataSet(elevationValues, getString(R.string.elevation));
        elevationDataSet.setDrawValues(false);
        elevationDataSet.setLineWidth(2f);
        elevationDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        elevationDataSet.setColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
        elevationDataSet.setDrawCircles(false);
        elevationDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        LineData elevationData = new LineData(elevationDataSet);
        mElevationChart.setData(elevationData);
        mElevationChart.invalidate();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mListener.setUpNavigation(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {

        /**
         * Retrieve the current GPX
         *
         * @return Gpx
         */
        Gpx getGpx();

        /**
         * Set up navigation arrow
         */
        void setUpNavigation(boolean upNavigation);
    }
}
