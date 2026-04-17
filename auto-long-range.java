public static class Paths {
    public PathChain MainChain;

    public Paths(Follower follower) {
      MainChain = follower.pathBuilder()
          .addPath(
            new BezierLine(
              new Pose(42.000, 8.000),
            new Pose(42.000, 8.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(42.000, 8.000),
            new Pose(40.000, 36.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(40.000, 36.000),
            new Pose(20.000, 36.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(20.000, 36.000),
            new Pose(42.000, 8.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(42.000, 8.000),
            new Pose(12.000, 15.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(210))
          .addPath(
            new BezierLine(
              new Pose(12.000, 15.000),
            new Pose(10.000, 9.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(210), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(10.000, 9.000),
            new Pose(42.000, 8.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(42.000, 8.000),
            new Pose(12.000, 15.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(210))
          .addPath(
            new BezierLine(
              new Pose(12.000, 15.000),
            new Pose(10.000, 9.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(210), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(10.000, 9.000),
            new Pose(42.000, 8.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(42.000, 8.000),
            new Pose(12.000, 15.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(210))
          .addPath(
            new BezierLine(
              new Pose(12.000, 15.000),
            new Pose(10.000, 9.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(210), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(10.000, 9.000),
            new Pose(42.000, 8.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(42.000, 8.000),
            new Pose(12.000, 15.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(210))
          .addPath(
            new BezierLine(
              new Pose(12.000, 15.000),
            new Pose(10.000, 9.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(210), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(10.000, 9.000),
            new Pose(42.000, 8.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .addPath(
            new BezierLine(
              new Pose(42.000, 8.000),
            new Pose(35.000, 8.000)
            )
          )
          .setLinearHeadingInterpolation(Math.toRadians(180), Math.toRadians(180))
          .build();
    }
  }