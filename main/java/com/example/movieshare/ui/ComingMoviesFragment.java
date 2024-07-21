package com.example.movieshare.ui;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.example.movieshare.MyApplication;
import com.example.movieshare.R;
import com.example.movieshare.adapter.ItemComingMoviesAdapter;
import com.example.movieshare.bean.SimpleSubjectBean;
import com.example.movieshare.listener.MyOnItemClickListener;
import com.example.movieshare.ui.MovieActivity;
import com.example.movieshare.util.Constant;
import com.example.movieshare.util.DataUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.example.movieshare.bean.Movie;
/**
 * A simple {@link Fragment} subclass.
 */
public class ComingMoviesFragment extends Fragment {




    private SwipeRefreshLayout swipeRf;
    private RecyclerView myRecyclerView;
    private List<SimpleSubjectBean> movieList = new ArrayList<SimpleSubjectBean>();//电影列表
    private ItemComingMoviesAdapter itemComingMoviesAdapter;//适配器

    private int start = 0;//默认从0开始获取数据
    private int count = 5;//每次显示的记录条数
    private Movie movie;//电影对象

    public ComingMoviesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_coming_movies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRf = (SwipeRefreshLayout) view.findViewById(R.id.swipeRf3);
        myRecyclerView = (RecyclerView) view.findViewById(R.id.myRecyclerView3);

        itemComingMoviesAdapter= new ItemComingMoviesAdapter(movieList,ComingMoviesFragment.this.getActivity());
        LinearLayoutManager llm = new LinearLayoutManager(ComingMoviesFragment.this.getActivity());
        llm.setOrientation(RecyclerView.VERTICAL);
        myRecyclerView.setLayoutManager(llm);
        myRecyclerView.setAdapter(itemComingMoviesAdapter);

        swipeRf.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh() {
                itemComingMoviesAdapter.upDown=0;
//                loadMovies();
                loadMovies_Net();
            }
        });
        myRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            int lastVisibleItem;
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if((newState==recyclerView.SCROLL_STATE_IDLE)
                        &&(lastVisibleItem+2>itemComingMoviesAdapter.getItemCount())
                        &&(itemComingMoviesAdapter.getItemCount()-1<movie.getTotal())){
                    itemComingMoviesAdapter.upDown=1;
//                    loadMovies();
                    loadMovies_Net();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                lastVisibleItem = lm.findLastVisibleItemPosition();
            }
        } );


        //设置单击回调
        itemComingMoviesAdapter.setmOnItemClickListener(new MyOnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                SimpleSubjectBean subjects = movieList.get(position);
                //Toast.makeText(MovieListFragment.this.getActivity(),"你选中的是第"+position+"个电影，名称："+subjects.getTitle(),Toast.LENGTH_LONG).show();

                //打开电影详细
                Intent intent = new Intent(ComingMoviesFragment.this.getActivity(), MovieActivity.class);
                intent.putExtra("subject_id",subjects.getId());
                intent.putExtra("image_url", subjects.getImages().getLarge());
                startActivity(intent);//打开新的activity

            }
        });

//        loadMovies();//首次加载电影
        loadMovies_Net();


    }

    //网络加载数据
    private void loadMovies_Net(){

        String mRequestUrl = Constant.API + Constant.COMING_SOON + "?start="+start+"&count="+count;

        Log.d("mRequestUrl", mRequestUrl);
        JsonObjectRequest request = new JsonObjectRequest(mRequestUrl,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int mTotalItem = response.getInt("total");
                            int mCountItem = response.getInt("count");

                            String moviesString = response.getString("subjects");
                            Gson gson = new Gson();
                            List<SimpleSubjectBean> movieList_net = gson.fromJson(moviesString,  new TypeToken<List<SimpleSubjectBean>>() {}.getType());

                            Movie movie_net = new Movie();
                            movie_net.setStart(start);
                            movie_net.setCount(mCountItem);
                            movie_net.setTotal(mTotalItem);
                            movie_net.setSubjects(movieList_net);

                            //封装消息，传递给主线程
                            Message message = Message.obtain();

                            message.obj = movie_net;
                            message.what = 100;//标识线程

                            handler.sendMessage(message);//发送消息给主线程

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("MovieListFragment",error.toString());
                        Toast.makeText(ComingMoviesFragment.this.getActivity(), error.toString(), Toast.LENGTH_SHORT).show();


                    }
                });
        MyApplication.addRequest(request, "MovieListFragment");
    }


    //建立一个Handler对象，用于主线程和子线程之间进行通信
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            //获取子线程传递过来的消息，取出进度更新音乐播放进度
            //msg.what 用于判断从那个线程传递过来的消息
            if(message.what==100){
                movie = (Movie)message.obj;
                itemComingMoviesAdapter.upDown=1;
                start = movie.getStart()+movie.getCount();
//                movieList.addAll(movie.getSubjects());
                itemComingMoviesAdapter.loadData(movie.getSubjects());

            }
        }
    };

    //本地加载数据
    private void loadMovies(){

        movie = DataUtil.loadAllMovies(ComingMoviesFragment.this.getActivity(),"movies.json",start,count);

        start = movie.getStart()+movie.getCount();
//        movieList = movie.getSubjects();
        movieList.addAll(movie.getSubjects());
        itemComingMoviesAdapter.loadData(movieList);

    }

}
