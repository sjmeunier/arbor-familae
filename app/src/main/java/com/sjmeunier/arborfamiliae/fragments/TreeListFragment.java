package com.sjmeunier.arborfamiliae.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sjmeunier.arborfamiliae.MainActivity;
import com.sjmeunier.arborfamiliae.treelist.OnTreeListViewClickListener;
import com.sjmeunier.arborfamiliae.treelist.OnTreeListViewDeleteListener;
import com.sjmeunier.arborfamiliae.treelist.OnTreeListViewLongPressListener;
import com.sjmeunier.arborfamiliae.R;
import com.sjmeunier.arborfamiliae.treelist.TreeListAdapter;
import com.sjmeunier.arborfamiliae.database.AppDatabase;
import com.sjmeunier.arborfamiliae.database.Tree;
import com.sjmeunier.arborfamiliae.gedcom.GedcomParser;

import java.util.List;

import static android.app.Activity.RESULT_OK;

public class TreeListFragment extends Fragment{

    private MainActivity mainActivity;
    private static final int GEDCOM_FILE_SELECT_CODE = 0;
    private TreeListAdapter treeListAdapter;

    private void showGedcomFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a file to import"), GEDCOM_FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(mainActivity, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void reloadTreeList() {
        treeListAdapter.treeList = mainActivity.database.treeDao().getAllTrees();
        treeListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GEDCOM_FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    GedcomLoader gedcomLoader = new GedcomLoader(mainActivity);
                    gedcomLoader.execute(uri);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
         View view = inflater.inflate(R.layout.fragment_treelist, container, false);

        mainActivity = (MainActivity)getActivity();

        List<Tree> treeData = mainActivity.database.treeDao().getAllTrees();
        ListView treeList = (ListView) view.findViewById(R.id.tree_list);
        treeListAdapter = new TreeListAdapter(mainActivity, treeData);
        treeList.setAdapter(treeListAdapter);
        treeListAdapter.setOnTreeListViewDeleteListener(new OnTreeListViewDeleteListener() {
            @Override
            public void OnTreeListViewDelete(int treeId, String treeName) {
                if (mainActivity.activeTree != null && mainActivity.activeTree.id == treeId) {

                    mainActivity.clearActiveTree();
                }
                mainActivity.clearRecentIndividuals(treeId);
                mainActivity.deleteTreePreferences(treeId);
                mainActivity.database.placeDao().deleteAllInTree(treeId);
                mainActivity.database.familyChildDao().deleteAllInTree(treeId);
                mainActivity.database.individualNoteDao().deleteAllInTree(treeId);
                mainActivity.database.familyNoteDao().deleteAllInTree(treeId);
                mainActivity.database.noteDao().deleteAllInTree(treeId);
                mainActivity.database.sourceDao().deleteAllInTree(treeId);
                mainActivity.database.individualSourceDao().deleteAllInTree(treeId);
                mainActivity.database.familySourceDao().deleteAllInTree(treeId);
                mainActivity.database.individualDao().deleteAllInTree(treeId);
                mainActivity.database.familyDao().deleteAllInTree(treeId);
                mainActivity.database.treeDao().delete(treeId);


                Toast.makeText(mainActivity, mainActivity.getResources().getText(R.string.message_tree_deleted) + " " + treeName, Toast.LENGTH_SHORT).show();
            }
        });

        treeListAdapter.setOnTreeListViewClickListener(new OnTreeListViewClickListener() {
            @Override
            public void OnTreeListViewClick(int treeId) {
                mainActivity.setActiveTree(treeId);
            }
        });

        treeListAdapter.setOnTreeListViewLongPressListener(new OnTreeListViewLongPressListener() {
            @Override
            public void OnTreeListViewLongPress(int treeId) {
                AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(mainActivity, R.style.MyProgressDialog));
                alert.setTitle( mainActivity.getResources().getText(R.string.dialog_rename_tree));

                final EditText input = new EditText(mainActivity);
                alert.setView(input);

                final int finalTreeId = treeId;
                alert.setPositiveButton( mainActivity.getResources().getText(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Tree tree = mainActivity.database.treeDao().getTree(finalTreeId);
                        tree.name = input.getText().toString();
                        mainActivity.database.treeDao().updateTree(tree);
                        reloadTreeList();
                    }
                });

                alert.setNegativeButton(mainActivity.getResources().getText(R.string.dialog_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog = alert.create();
                alertDialog.show();
            }
        });
        TextView emptyView = view.findViewById(R.id.empty_tree_list);
        treeList.setEmptyView(emptyView);

        FloatingActionButton createTreeButton = (FloatingActionButton) view.findViewById(R.id.button_create_tree);
        createTreeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGedcomFileChooser();
           }
        });
        setHasOptionsMenu(false);

        return view;
    }


    private class GedcomLoader extends AsyncTask<Uri, Integer, String> {
        private Context context;
        private ProgressDialog progressDialog;

        public GedcomLoader (Context context){
            this.context = context;
        }

        @Override
        protected String doInBackground(Uri... params) {
            Uri gedcomResource = params[0];

            AppDatabase database = AppDatabase.getDatabase(context.getApplicationContext());
            String message = "";
            int treeId = 0;
            try {
                GedcomParser parser = new GedcomParser(database, context.getContentResolver());
                treeId = parser.parseGedcom(context, gedcomResource);
                message = "Finished importing tree";
             } catch (Exception ex) {
                ex.printStackTrace();
                message = "Unable to import tree";
             }

            return message;
        }

        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
            reloadTreeList();
            this.context = null;
        }
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context, R.style.MyProgressDialog);
            progressDialog.setTitle(context.getResources().getText(R.string.progress_import));
            progressDialog.setMessage(context.getResources().getText(R.string.progress_pleasewait));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }
}
