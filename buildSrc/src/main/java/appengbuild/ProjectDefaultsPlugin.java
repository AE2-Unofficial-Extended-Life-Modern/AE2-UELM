package appengbuild;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.util.Collections;

public class ProjectDefaultsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.setGroup("org.appliedenergistics");

        var java = project.getExtensions().getByType(JavaPluginExtension.class);

        // Enable javadoc and sources
        java.withSourcesJar();
        java.withJavadocJar();

        java.toolchain(spec -> {
            spec.getLanguageVersion().set(project.getProviders().gradleProperty("java_version").map(JavaLanguageVersion::of));
        });

        // ensure everything uses UTF-8 and not some random codepage chosen by Gradle
        project.getTasks().withType(JavaCompile.class, ProjectDefaultsPlugin::setupCompiler);

        String projectVersion = getProjectVersion(project);
        project.setVersion(projectVersion);
        project.getLogger().lifecycle("AE2 Version: {}", projectVersion);
        project.getTasks().register("printProjectVersion", PrintProjectVersion.class);
    }

    private String getProjectVersion(Project project) {
        String tag = System.getenv("TAG");
        String pr = System.getenv("PR_NUMBER");
        String commit = System.getenv("COMMIT_SHA");

        if (tag != null && !tag.isEmpty()) {
            if (!tag.startsWith("neoforge/v")) {
                throw new IllegalArgumentException("Tags for the neoforge version should start with neoforge/: " + tag);
            }
            return tag.substring("neoforge/v".length());
        } else if (pr != null && !pr.isEmpty()) {
            return "0.0.0-pr." + pr + "-SNAPSHOT";
        } else if (commit != null && !commit.isEmpty()) {
            String base = System.getenv("BASE_VERSION");
            return (base == null || base.isEmpty() ? "0.0.0" : base) + "-sha." + commit + "-SNAPSHOT";
        } else {
            // Use Gradle's Provider API for configuration cache support
            var versionProvider = project.getProviders()
                    .gradleProperty("version")
                    .orElse("")
                    .flatMap(version -> {
                        if (!version.isBlank()) {
                            return project.getProviders().provider(() -> version);
                        }

                        // Custom version source provider
                        return project.getProviders().of(ProjectVersionSource.class, spec -> {
                            spec.getParameters()
                                    .getDefaultBranches()
                                    .addAll(
                                            "main",
                                            "neoforge/" + project.getProperties().get("minecraft_version")
                                    );
                        });
                    });

            return versionProvider.get();
        }
    }

    private static void setupCompiler(JavaCompile task) {
        var options = task.getOptions();
        options.setEncoding("UTF-8");
        options.setDeprecation(false);
        Collections.addAll(
                options.getCompilerArgs(),
                "-Xmaxerrs", "9999"
        );
    }
}
