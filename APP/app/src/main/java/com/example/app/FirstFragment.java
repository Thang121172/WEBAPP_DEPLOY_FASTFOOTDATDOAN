package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Stubbed FirstFragment: kept as a minimal fragment to avoid build errors while
 * navigation
 * has been migrated to HomeFragment. This intentionally does not use the old
 * bindings or
 * navigation actions.
 */
public class FirstFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Simple empty view
        return new View(requireContext());
    }
}