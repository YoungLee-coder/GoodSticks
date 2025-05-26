package cn.younglee.goodsticks.ui.note;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Locale;

import cn.younglee.goodsticks.R;
import cn.younglee.goodsticks.data.entity.Note;
import cn.younglee.goodsticks.databinding.ItemNoteBinding;

public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {
    
    private final NoteClickListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINESE);
    
    public interface NoteClickListener {
        void onNoteClick(long noteId);
        void onNoteLongClick(long noteId);
    }
    
    public NoteAdapter(NoteClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    
    private static final DiffUtil.ItemCallback<Note> DIFF_CALLBACK = new DiffUtil.ItemCallback<Note>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getContent().equals(newItem.getContent()) &&
                   oldItem.getModifiedDate().equals(newItem.getModifiedDate()) &&
                   oldItem.isPinned() == newItem.isPinned();
        }
    };
    
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = getItem(position);
        holder.bind(note);
    }
    
    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoteBinding binding;
        
        NoteViewHolder(@NonNull ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNoteClick(getItem(position).getId());
                }
            });
            
            binding.getRoot().setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNoteLongClick(getItem(position).getId());
                    return true;
                }
                return false;
            });
        }
        
        void bind(Note note) {
            // 标题
            if (note.getTitle() != null && !note.getTitle().isEmpty()) {
                binding.tvTitle.setText(note.getTitle());
                binding.tvTitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvTitle.setVisibility(View.GONE);
            }
            
            // 内容
            if (note.getContent() != null && !note.getContent().isEmpty()) {
                binding.tvContent.setText(note.getContent());
                binding.tvContent.setVisibility(View.VISIBLE);
            } else {
                binding.tvContent.setVisibility(View.GONE);
            }
            
            // 时间
            binding.tvTime.setText(dateFormat.format(note.getModifiedDate()));
            
            // 图片
            if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
                binding.ivImage.setVisibility(View.VISIBLE);
                Glide.with(binding.getRoot())
                        .load(note.getImagePath())
                        .centerCrop()
                        .into(binding.ivImage);
            } else {
                binding.ivImage.setVisibility(View.GONE);
            }
            
            // 置顶标记
            binding.ivPinned.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
            
            // 背景颜色
            if (note.getColor() != 0) {
                binding.cardView.setCardBackgroundColor(note.getColor());
            } else {
                binding.cardView.setCardBackgroundColor(
                        binding.getRoot().getContext().getColor(R.color.card_background));
            }
        }
    }
} 