package jaelyn.pm;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Shader;

/**
 * Created by zaric on 17-03-22.
 */

public class PMElement {

    /**
     *PM移动轨迹
     */
    Path path;
    float pathLenght;
    PathMeasure pathMeasure;

    float x, y;
    PointF centerPoint = new PointF();
    /**
     * 记录关键点
     */
    float[] point;

    /**
     * 衍生点的数据
     */
    double[] dervie;

    /** 移动相关常量 **/

    /**
     * 动画播放的时间
     */
    float duration;

    long starTime;

    float progress;
    double angle;

    double angleStar = 0;
}
