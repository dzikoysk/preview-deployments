@import com.dzikoysk.preview.ui.UiModel
@import com.dzikoysk.preview.ui.RunningEnvironmentModel
@import com.dzikoysk.preview.ui.ServiceModel
@param UiModel model

<!DOCTYPE html>
<html lang="en">
    <head>
        <title>Feature-preview</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="stylesheet" href="https://rsms.me/inter/inter.css">
        <script src="https://cdn.tailwindcss.com"></script>
        <style>
        * {
            font-family: 'Inter', sans-serif;
        }
        #editor {
            font-family: monospace;
        }
        .ace_editor, .ace_editor *{
            font-family: "Monaco", "Menlo", "Ubuntu Mono", "Droid Sans Mono", "Consolas", monospace !important;
            font-size: 14px !important;
            font-weight: 400 !important;
            letter-spacing: 0 !important;
        }
        </style>
    </head>
    <body class="min-h-full bg-zinc-900 text-white min-w-full">
        <div class="container-lg mx-auto">
            @if(!model.getLoggedIn())
                <div class="flex justify-center pt-10">
                    <form
                        method="post"
                        action="/api/ui/login"
                        class="flex flex-col content-center w-96 spacing-4 bg-zinc-800 px-6 py-10 rounded-xl"
                    >
                        <h1 class="font-bold text-lg text-center text-white">
                            Access the dashboard
                        </h1>
                        <label for="username" class="pt-6">
                            <input name="username" type="text" class="border w-full rounded-lg px-2 py-1 text-black text-sm" placeholder="Username">
                        </label>
                        <label class="py-2">
                            <input name="password" type="password" class="border w-full rounded-lg px-2 py-1 text-black text-sm" placeholder="Password">
                        </label>
                        <button type="submit" class="border bg-white text-black rounded-lg py-1 text-sm">Login</button>
                    </form>
                </div>
            @else
                <div class="flex justify-center pt-10">
                    <div class="flex flex-col content-center w-5/6 lg:w-3/5 spacing-4 bg-zinc-800 text-sm px-6 py-10 rounded-xl">
                        <div class="flex">
                            <h1 class="font-bold text-xl text-center text-white">
                                Dashboard
                            </h1>
                            <div class="flex-grow"></div>
                            <a href="/api/ui/logout" class="text-red-400">Logout</a>
                        </div>
                        <div class="pt-2">
                            <span class="pr-8">Logged as </span>
                            <span class="bg-zinc-900 px-4 py-1 rounded-lg">${model.getUsername()}</span>
                        </div>
                        <div class="pt-2">
                            <span class="pr-2 w-10">Webhook URL </span>
                            <span id="webhook-url" class="bg-zinc-900 px-4 py-1 rounded-lg"></span>
                        </div>
                        <div class="pt-9">
                            <div class="flex place-content-between">
                                <h2 class="font-bold text-lg text-white w-1/2">
                                    Active environments
                                </h2>
                                <form method="post" action="/api/ui/preview" class="flex w-1/2">
                                    <label for="branch" class="w-2/3 h-6">
                                        <input
                                            name="branch"
                                            type="text"
                                            class="w-full border rounded-md rounded-r-none px-2 py-0.5 bg-zinc-900 text-sm text-white text-black"
                                            placeholder="Trigger branch"
                                        />
                                    </label>
                                    <button class="w-1/3 h-6 border box-content bg-zinc-900 rounded-lg rounded-l-none text-sm py-0">Trigger</button>
                                </form>
                            </div>
                            <div class="flex flex-col justify-center">
                                @for(RunningEnvironmentModel environment : model.getActiveEnvironments())
                                    <div class="flex flex-row bg-zinc-900 px-4 py-2 rounded-md mt-2">
                                        <div class="flex flex-col w-full">
                                            <div class="flex flex-row">
                                                <div class="font-bold text-white">
                                                    ${environment.getBranch()}
                                                </div>
                                            </div>
                                            @for(ServiceModel service : environment.getServices())
                                                <div class="flex items-center place-content-between w-full">
                                                    <div class="flex items-center">
                                                        @if(service.getActive())
                                                            <div class="bg-green-400 rounded-full w-2 h-2"></div>
                                                        @else
                                                            <div class="bg-orange-400 rounded-full w-2 h-2"></div>
                                                        @endif
                                                        <span class="pl-3">
                                                            ${service.getName()}
                                                        </span>
                                                    </div>
                                                    <div>
                                                        Published at
                                                        @if(service.getUrl() != null)
                                                            <a href="${service.getUrl()}">${service.getUrl()}</a>
                                                        @else
                                                            None
                                                        @endif
                                                    </div>
                                                </div>
                                            @endfor
                                        </div>
                                    </div>
                                @endfor
                                @if(model.getActiveEnvironments().isEmpty())
                                    <div class="flex flex-row bg-zinc-900 px-4 py-2 rounded-md mt-2">
                                        <div class="flex flex-col w-full">
                                            <div class="flex flex-row">
                                                <div class="font-bold text-white text-sm">
                                                    ~ No active environments ~
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                @endif
                            </div>
                        </div>
                        <div class="flex flex-col pt-9 pb-4">
                            <h2 class="font-bold text-lg text-white w-1/2 pb-4">
                                Configuration
                            </h2>
<div id="editor" class="w-full rounded-xl">${model.getConfig()}</div>
                        </div>
                        <button id="update-config" class="bg-zinc-900 rounded-lg py-2 text-sm">
                            Update configuration
                        </button>
                    </div>
                </div>
            @endif
        </div>
        <div class="absolute bottom-4 mx-auto w-full">
            <div class="w-full text-center">
                <a href="https://github.com/dzikoysk" class="text-xs">
                    powered by monke solutions
                </a>
            </div>
        </div>
    </body>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.23.4/ace.min.js"></script>
    <!--suppress JSUnresolvedReference -->
    <script>
        const webhookUrl = document.getElementById('webhook-url')

        if (webhookUrl) {
            webhookUrl.innerText = window.location.origin + '${model.getWebhookUrl()}';
        }

        if (document.getElementById("editor") != null) {
            const editor = ace.edit("editor");
            editor.setTheme("ace/theme/github_dark");
            editor.session.setMode("ace/mode/yaml");
            editor.setOptions({
                indentedSoftWrap: false,
                tabSize: 2,
                maxLines: "Infinity",
                highlightActiveLine: false,
                showPrintMargin: false,
            })
            editor.renderer.setPadding(15, 15, 0, 0);
            editor.renderer.setScrollMargin(15, 15, 0, 0);

            document.getElementById("update-config").addEventListener("click", () => {
                fetch("/api/ui/config", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        config: editor.getValue()
                    })
                })
                .then(() => window.location.reload())
                .catch(error => {
                    if (error.toString().includes("NetworkError")) {
                        setTimeout(() => window.location.reload(), 1000)
                    } else {
                        alert("Failed to update configuration: " + error)
                    }
                })
            })
        }
    </script>
</html>