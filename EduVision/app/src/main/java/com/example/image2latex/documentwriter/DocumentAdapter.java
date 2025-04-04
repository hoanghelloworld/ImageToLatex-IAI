package com.example.image2latex.documentwriter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.image2latex.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<Document> documents = new ArrayList<>();
    private DocumentClickListener listener;

    public DocumentAdapter(DocumentClickListener listener) {
        this.listener = listener;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documents.get(position);
        holder.bind(document);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView dateTextView;
        private ImageButton menuButton;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.document_title);
            dateTextView = itemView.findViewById(R.id.document_date);
            menuButton = itemView.findViewById(R.id.document_menu);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDocumentClick(documents.get(position));
                }
            });

            menuButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDocumentMenuClick(v, documents.get(position));
                }
            });
        }

        public void bind(Document document) {
            titleTextView.setText(document.getTitle());
            
            // Format date
            String formattedDate = "Last modified: " + dateFormat.format(document.getModifiedDate());
            dateTextView.setText(formattedDate);
        }
    }

    public interface DocumentClickListener {
        void onDocumentClick(Document document);
        void onDocumentMenuClick(View view, Document document);
    }
} 