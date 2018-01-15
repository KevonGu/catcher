from com.embraiz.grap.catcher import JythonExecutor

data = JythonExecutor.getData()
projects = data.get("projects")

list = projects.values();

