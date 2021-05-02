package video.overlay.picture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.List;

import me.littlecheesecake.croplayout.EditPhotoView;
import me.littlecheesecake.croplayout.EditableImage;
import me.littlecheesecake.croplayout.handler.OnBoxChangedListener;
import me.littlecheesecake.croplayout.model.ScalableBox;
import video.overlay.picture.databinding.FragmentPlayerBinding;

public class PlayerFragment extends Fragment {

    FragmentPlayerBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
       binding = FragmentPlayerBinding.inflate(inflater, container, false);
       View view = binding.getRoot();

      final EditableImage image = new EditableImage(getArguments().getString("image"));

        ScalableBox box1 = new ScalableBox(25,180,640,880);
        ScalableBox box2 = new ScalableBox(2,18,680,880);
        ScalableBox box3 =  new ScalableBox(250,80,400,880);
        List<ScalableBox> boxes = new ArrayList<>();
        boxes.add(box1);
        boxes.add(box2);
        boxes.add(box3);
        image.setBoxes(boxes);
        binding.imageView.initView(getActivity(), image);

        binding.imageView.setOnBoxChangedListener(new OnBoxChangedListener() {
            @Override
            public void onChanged(int x1, int y1, int x2, int y2) {
                binding.tv.setText("box: [" + x1 + "," + y1 +"],[" + x2 + "," + y2 + "]");
            }
        });
       return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);





    }
}