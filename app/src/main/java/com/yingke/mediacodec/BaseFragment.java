package com.yingke.mediacodec;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yingke.mediacodec.player.PlayerLog;


public class BaseFragment extends Fragment {


    public BaseFragment() {


    }

    @Override
    public void onAttach(Activity activity) {
        PlayerLog.d(getClass().getSimpleName(), "onAttach to " + activity.getClass().getSimpleName());
        super.onAttach(activity);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        PlayerLog.d(getClass().getSimpleName(), "onCreate");
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        PlayerLog.d(getClass().getSimpleName(), "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        PlayerLog.d(getClass().getSimpleName(), "onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PlayerLog.d(getClass().getSimpleName(), "onViewCreated");
    }

    @Override
    public void onStart() {
        PlayerLog.d(getClass().getSimpleName(), "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        PlayerLog.d(getClass().getSimpleName(), "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        PlayerLog.d(getClass().getSimpleName(), "onPause");
        super.onPause();

    }

    @Override
    public void onStop() {
        PlayerLog.d(getClass().getSimpleName(), "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PlayerLog.d(getClass().getSimpleName(), "onDestroyView");
    }

    @Override
    public void onDestroy() {
        PlayerLog.d(getClass().getSimpleName(), "onDestroy");
        super.onDestroy();

    }

    @Override
    public void onDetach() {
        PlayerLog.d(getClass().getSimpleName(), "onDetach from "
                + getActivity().getClass().getSimpleName());
        super.onDetach();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

    }
}
