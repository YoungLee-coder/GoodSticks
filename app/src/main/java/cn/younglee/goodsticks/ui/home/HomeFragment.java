package cn.younglee.goodsticks.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import cn.younglee.goodsticks.databinding.FragmentHomeBinding;
import cn.younglee.goodsticks.ui.note.EditNoteActivity;
import cn.younglee.goodsticks.ui.note.NoteAdapter;
import cn.younglee.goodsticks.ui.note.NoteViewModel;

public class HomeFragment extends Fragment {
    
    private FragmentHomeBinding binding;
    private NoteViewModel noteViewModel;
    private NoteAdapter adapter;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        noteViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        
        setupRecyclerView();
        setupFab();
        setupSearch();
        observeNotes();
    }
    
    private void setupRecyclerView() {
        // 使用瀑布流布局，类似iOS的卡片式设计
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL);
        binding.recyclerView.setLayoutManager(layoutManager);
        
        adapter = new NoteAdapter(new NoteAdapter.NoteClickListener() {
            @Override
            public void onNoteClick(long noteId) {
                // 打开编辑页面
                Intent intent = new Intent(getActivity(), EditNoteActivity.class);
                intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, noteId);
                startActivity(intent);
            }
            
            @Override
            public void onNoteLongClick(long noteId) {
                // 长按显示选项菜单
                showNoteOptionsDialog(noteId);
            }
        });
        
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void setupFab() {
        binding.fabAddNote.setOnClickListener(v -> {
            // 创建新笔记
            Intent intent = new Intent(getActivity(), EditNoteActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    observeNotes();
                } else {
                    noteViewModel.searchNotes(query).observe(getViewLifecycleOwner(), notes -> {
                        adapter.submitList(notes);
                        updateEmptyView(notes == null || notes.isEmpty());
                    });
                }
            }
        });
    }
    
    private void observeNotes() {
        noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            updateEmptyView(notes == null || notes.isEmpty());
        });
    }
    
    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showNoteOptionsDialog(long noteId) {
        String[] options = {"置顶/取消置顶", "删除"};
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("选择操作")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 切换置顶状态
                        noteViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
                            if (notes != null) {
                                for (cn.younglee.goodsticks.data.entity.Note note : notes) {
                                    if (note.getId() == noteId) {
                                        noteViewModel.updatePinStatus(noteId, !note.isPinned());
                                        break;
                                    }
                                }
                            }
                        });
                    } else if (which == 1) {
                        // 删除确认
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("删除笔记")
                                .setMessage("确定要删除这条笔记吗？")
                                .setPositiveButton("删除", (dialog2, which2) -> {
                                    noteViewModel.deleteById(noteId);
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    }
                })
                .show();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 