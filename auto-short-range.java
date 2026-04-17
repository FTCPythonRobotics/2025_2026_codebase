public static class Paths {
    public PathChain MainChain;

    public Paths(Follower follower) {
      MainChain = follower.pathBuilder()
          .addPath(
            new BezierLine(
              new Pose(20.000, 120.000),
            new Pose(53.000, 88.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(144), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(53.000, 88.000),
            new Pose(40.000, 59.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(40.000, 59.000),
            new Pose(20.000, 59.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(20.000, 59.000),
            new Pose(25.000, 69.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(25.000, 69.000),
            new Pose(15.000, 69.000)
            )
          )
          .setTangentHeadingInterpolation()
          .addPath(
            new BezierCurve(
              new Pose(15.000, 69.000),
            new Pose(30.000, 68.000),
            new Pose(50.000, 70.000),
            new Pose(53.000, 88.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(53.000, 88.000),
            new Pose(40.000, 83.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(40.000, 83.000),
            new Pose(18.000, 83.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(18.000, 83.000),
            new Pose(53.000, 88.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(53.000, 88.000),
            new Pose(40.000, 35.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(40.000, 35.000),
            new Pose(20.000, 35.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(20.000, 35.000),
            new Pose(65.000, 110.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .build();
    }
  }